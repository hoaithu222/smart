<#
  Cai du moi truong cho do an The sinh vien (Smart Card) tren mot may Windows moi.
  File nay VIET KHONG DAU co y (PowerShell 5.1 doc sai file UTF-8 khong co BOM).

  Cai: JDK 8, SQL Server 2022 Express (instance SQLEXPRESS, dang nhap sa, TCP 1433),
       SSMS 22, Apache NetBeans 32 (ban zip), bat dich vu Smart Card (PC/SC).
  Neu co file zip JavaCardKit (JCIDE + PyApduTool): giai nen vao C:\JavaCardKit.
  KHONG tai JCIDE duoc (javacardos.com can ban tu vao trinh duyet) - xem 07-cai-dat-chay-that.md muc 5.

  Chay: chuot phai PowerShell -> Run as administrator, roi:
      powershell -ExecutionPolicy Bypass -File .\cai-dat-may-moi.ps1
      powershell -ExecutionPolicy Bypass -File .\cai-dat-may-moi.ps1 -JavaCardKitZip C:\Users\ban\Downloads\JCKit.zip -HostDir D:\sbc\smart\code\host

  Buoc nao da cai roi se tu bo qua, chay lai nhieu lan khong sao.
  CHUA chay tron ven tren may khac: tung buoc da chay that tren may soan (22/11/2026), ca file thi moi kiem tra cu phap.
#>
#Requires -RunAsAdministrator
param(
    [string]$JavaCardKitZip = '',
    [string]$HostDir = '',
    [switch]$SkipSsms
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$work = Join-Path $env:TEMP 'smartcard-setup'
New-Item -ItemType Directory -Force $work | Out-Null

function Say($m) { Write-Host ("[{0}] {1}" -f (Get-Date -Format HH:mm:ss), $m) }

function Assert-MicrosoftSigned($path) {
    $s = Get-AuthenticodeSignature $path
    if ($s.Status -ne 'Valid' -or $s.SignerCertificate.Subject -notmatch 'O=Microsoft Corporation') {
        throw "Chu ky so khong hop le: $path ($($s.Status))"
    }
}

$hasWinget = [bool](Get-Command winget -ErrorAction SilentlyContinue)

# ---------- 1. JDK 8 ----------
$jdk8 = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-8*' -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $jdk8 -and (Test-Path 'D:\Tools\jdk8\bin\javac.exe')) { $jdk8 = Get-Item 'D:\Tools\jdk8' }
if ($jdk8) {
    Say "JDK 8 da co: $($jdk8.FullName)"
} else {
    if (-not $hasWinget) { throw 'Khong co winget. Cai JDK 8 tay: https://adoptium.net/temurin/releases/?version=8' }
    Say 'Cai JDK 8 (Temurin)...'
    winget install -e --id EclipseAdoptium.Temurin.8.JDK --source winget --silent --accept-package-agreements --accept-source-agreements
    if ($LASTEXITCODE -ne 0) { throw "winget JDK 8 loi, ma $LASTEXITCODE" }
}

# ---------- 2. SQL Server 2022 Express ----------
$saPwd = $null
if (Get-Service 'MSSQL$SQLEXPRESS' -ErrorAction SilentlyContinue) {
    Say 'SQL Server instance SQLEXPRESS da co - bo qua cai moi.'
} else {
    $sec = Read-Host 'Dat mat khau cho tai khoan sa (>= 8 ky tu, co HOA, thuong, so, ky hieu, KHONG co dau cach/nhay kep)' -AsSecureString
    $saPwd = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec))
    if ($saPwd.Length -lt 8 -or $saPwd -cnotmatch '[A-Z]' -or $saPwd -cnotmatch '[a-z]' -or $saPwd -notmatch '\d' -or $saPwd -notmatch '[^A-Za-z0-9]' -or $saPwd -match '[\s"]') {
        throw 'Mat khau khong dat yeu cau cua SQL Server'
    }
    $ssei = Join-Path $work 'SQL2022-SSEI-Expr.exe'
    $media = Join-Path $work 'sqlmedia'
    New-Item -ItemType Directory -Force $media | Out-Null
    Say 'Tai bo cai SQL Server (SSEI)...'
    Invoke-WebRequest -UseBasicParsing 'https://download.microsoft.com/download/29654887-7cde-4397-bba3-d7f087970845/SQL2022-SSEI-Expr.exe' -OutFile $ssei
    Assert-MicrosoftSigned $ssei
    Say 'Tai bo cai Core (~280 MB, vai phut)...'
    # Luu y: o buoc Download KHONG them /IACCEPTSQLSERVERLICENSETERMS (se bao loi)
    Start-Process $ssei -Wait -ArgumentList '/ACTION=Download', "/MEDIAPATH=$media", '/MEDIATYPE=Core', '/QUIET'
    $core = Get-ChildItem $media -Filter 'SQLEXPR*.exe' | Select-Object -First 1
    if (-not $core) { throw 'Khong tai duoc bo cai Core cua SQL Server' }
    Assert-MicrosoftSigned $core.FullName
    Say 'Cai SQL Server Express (vai phut)...'
    $me = "$env:USERDOMAIN\$env:USERNAME"
    $p = Start-Process $core.FullName -Wait -PassThru -ArgumentList '/Q', '/ACTION=Install', '/IACCEPTSQLSERVERLICENSETERMS', '/FEATURES=SQLENGINE', '/INSTANCENAME=SQLEXPRESS', '/SECURITYMODE=SQL', "/SAPWD=$saPwd", '/TCPENABLED=1', "/SQLSYSADMINACCOUNTS=$me", '/UpdateEnabled=0'
    if ($p.ExitCode -ne 0 -and $p.ExitCode -ne 3010) { throw "Cai SQL Server loi, ma $($p.ExitCode). Xem log: C:\Program Files\Microsoft SQL Server\160\Setup Bootstrap\Log" }
    Say 'Dat cong TCP 1433 co dinh...'
    $base = 'HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server'
    $inst = (Get-ItemProperty "$base\Instance Names\SQL").SQLEXPRESS
    $ipall = "$base\$inst\MSSQLServer\SuperSocketNetLib\Tcp\IPAll"
    Set-ItemProperty $ipall TcpPort '1433'
    Set-ItemProperty $ipall TcpDynamicPorts ''
    Restart-Service 'MSSQL$SQLEXPRESS' -Force
}

# ---------- 3. Dich vu Smart Card (PC/SC) ----------
Set-Service SCardSvr -StartupType Automatic
Start-Service SCardSvr -ErrorAction SilentlyContinue
Say "Dich vu Smart Card: $((Get-Service SCardSvr).Status)"

# ---------- 4. NetBeans 32 (zip) ----------
$nbDir = 'C:\Tools\netbeans-32'
if (Test-Path "$nbDir\bin\netbeans64.exe") {
    Say "NetBeans da co: $nbDir"
} else {
    $zip = Join-Path $work 'netbeans-32-bin.zip'
    $urls = @('https://dlcdn.apache.org/netbeans/netbeans/32/netbeans-32-bin.zip', 'https://archive.apache.org/dist/netbeans/netbeans/32/netbeans-32-bin.zip')
    $ok = $false
    foreach ($u in $urls) {
        try {
            Say "Tai NetBeans 32 (~500 MB) tu $u ..."
            Invoke-WebRequest -UseBasicParsing $u -OutFile $zip
            $want = ((Invoke-WebRequest -UseBasicParsing "$u.sha512").Content -split '\s+')[0].Trim().ToLower()
            $got = (Get-FileHash $zip -Algorithm SHA512).Hash.ToLower()
            if ($want -ne $got) { throw 'SHA-512 khong khop' }
            $ok = $true; break
        } catch { Say "Loi: $($_.Exception.Message)" }
    }
    if (-not $ok) { throw 'Khong tai duoc NetBeans. Tai tay o https://netbeans.apache.org/front/main/download/' }
    $tmp = 'C:\Tools\_nbtmp'
    New-Item -ItemType Directory -Force $tmp | Out-Null
    Expand-Archive $zip -DestinationPath $tmp -Force
    Move-Item "$tmp\netbeans" $nbDir
    Remove-Item $tmp -Recurse -Force
    $lnk = (New-Object -ComObject WScript.Shell).CreateShortcut("$env:ProgramData\Microsoft\Windows\Start Menu\Programs\Apache NetBeans 32.lnk")
    $lnk.TargetPath = "$nbDir\bin\netbeans64.exe"; $lnk.WorkingDirectory = "$nbDir\bin"; $lnk.Save()
    Say "Da cai NetBeans 32 tai $nbDir"
}

# ---------- 5. SSMS ----------
if ($SkipSsms) {
    Say 'Bo qua SSMS (-SkipSsms).'
} elseif (Test-Path 'C:\Program Files\Microsoft SQL Server Management Studio 22') {
    Say 'SSMS 22 da co.'
} elseif ($hasWinget) {
    Say 'Cai SSMS 22 (vai phut)...'
    winget install -e --id Microsoft.SQLServerManagementStudio.22 --source winget --silent --accept-package-agreements --accept-source-agreements
    if ($LASTEXITCODE -ne 0) { Say "SSMS cai khong thanh cong (ma $LASTEXITCODE) - co the cai sau." }
} else {
    Say 'Khong co winget: cai SSMS tay o https://learn.microsoft.com/sql/ssms/download-sql-server-management-studio-ssms'
}

# ---------- 6. JavaCardKit (neu co file zip) ----------
if ($JavaCardKitZip) {
    if (-not (Test-Path $JavaCardKitZip)) { throw "Khong thay file: $JavaCardKitZip" }
    if (Test-Path 'C:\JavaCardKit\JDK\bin\javac.exe') {
        Say 'C:\JavaCardKit da co - khong ghi de.'
    } else {
        $tmp = Join-Path $work 'jckit'
        New-Item -ItemType Directory -Force $tmp | Out-Null
        Expand-Archive $JavaCardKitZip -DestinationPath $tmp -Force
        $javac = Get-ChildItem $tmp -Recurse -Filter 'javac.exe' | Where-Object { $_.FullName -match '\\JDK\\bin\\' } | Select-Object -First 1
        if (-not $javac) { throw 'Trong file zip khong thay JDK\bin\javac.exe: khong phai bo JavaCardKit dung cau truc.' }
        $root = $javac.Directory.Parent.Parent.FullName
        New-Item -ItemType Directory -Force 'C:\JavaCardKit' | Out-Null
        Copy-Item "$root\*" 'C:\JavaCardKit' -Recurse -Force
        Say 'Da chep JavaCardKit vao C:\JavaCardKit'
    }
    Say ('Kiem tra api.jar: ' + (Test-Path 'C:\JavaCardKit\SDK\Tools\JCIDE\lib\java_card_kit\2_2_2\lib\api.jar'))
} elseif (-not (Test-Path 'C:\JavaCardKit\JDK\bin\javac.exe')) {
    Say 'CHUA co JavaCardKit (JCIDE + PyApduTool). Lay file zip tu co/ban nhom roi chay lai voi -JavaCardKitZip <duong dan>.'
}

# ---------- 7. Cau hinh phan mem (app.local.properties) ----------
if ($HostDir) {
    $src = Join-Path $HostDir 'app.properties'
    $dst = Join-Path $HostDir 'app.local.properties'
    if (-not (Test-Path $src)) { throw "Khong thay $src" }
    if (Test-Path $dst) {
        Say "$dst da co - khong ghi de."
    } elseif (-not $saPwd) {
        Say 'Chua nhap mat khau sa trong lan chay nay: tu tao app.local.properties va dien db.password.'
    } else {
        $lines = Get-Content $src -Encoding UTF8 | ForEach-Object {
            if ($_ -match '^card\.mode=') { 'card.mode=jcardsim' }
            elseif ($_ -match '^db\.password=') { "db.password=$saPwd" }
            else { $_ }
        }
        [IO.File]::WriteAllLines($dst, $lines, (New-Object Text.UTF8Encoding($false)))
        Say "Da tao $dst (card.mode=jcardsim cho toi khi JCIDE chay; doi thanh pcsc sau)."
    }
}

Say 'XONG. Chay thu: code\host\chay-phan-mem.cmd'
