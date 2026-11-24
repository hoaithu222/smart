<#
  Kiểm tra toàn bộ môi trường của bài Thẻ sinh viên (Smart Card) và in bảng ĐẠT / CẢNH BÁO / LỖI.
  Không sửa gì trên máy (chỉ đọc). Mật khẩu sa lấy từ code\host\app.local.properties, không in ra.

  Chạy:   powershell -ExecutionPolicy Bypass -File .\kiem-tra-moi-truong.ps1
  Thêm:   -ChayKiemThu   chạy luôn 3 bộ kiểm thử (SQLite+giao diện, SQL Server thật, build CAP)
  Mã thoát: 0 = không có LỖI, 1 = có LỖI.
  (File lưu UTF-8 có BOM để Windows PowerShell 5.1 đọc đúng tiếng Việt.)
#>
param([switch]$ChayKiemThu)

try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$host_dir = Join-Path $root 'code\host'
$script:fail = 0; $script:warn = 0; $script:ok = 0

function Show($status, $name, $detail) {
    $color = switch ($status) { 'DAT' { 'Green' } 'CANH BAO' { 'Yellow' } default { 'Red' } }
    $label = switch ($status) { 'DAT' { 'ĐẠT     ' } 'CANH BAO' { 'CẢNH BÁO' } default { 'LỖI     ' } }
    switch ($status) { 'DAT' { $script:ok++ } 'CANH BAO' { $script:warn++ } default { $script:fail++ } }
    Write-Host ('[{0}] {1}' -f $label, $name) -ForegroundColor $color -NoNewline
    if ($detail) { Write-Host ('  - ' + $detail) -ForegroundColor DarkGray } else { Write-Host '' }
}
function Head($t) { Write-Host ''; Write-Host ('== ' + $t) -ForegroundColor Cyan }

function Prop($file, $key) {
    if (-not (Test-Path $file)) { return $null }
    foreach ($l in Get-Content -Encoding UTF8 $file) {
        if ($l -match ('^\s*' + [regex]::Escape($key) + '\s*=\s*(.*)$')) { return $Matches[1].Trim() }
    }
    return $null
}

# ---------------------------------------------------------------- Java
Head 'Java'
$jdk8 = 'C:\Program Files\Eclipse Adoptium\jdk-8.0.512.9-hotspot'; if (-not (Test-Path "$jdk8\bin\javac.exe") -and (Test-Path 'D:\Tools\jdk8\bin\javac.exe')) { $jdk8 = 'D:\Tools\jdk8' }
if (Test-Path "$jdk8\bin\javac.exe") {
    $v = (& "$jdk8\bin\javac.exe" -version 2>&1) -join ' '
    Show 'DAT' 'JDK 8 (biên dịch applet -source 1.5, đúng JCIDE/Java Card 2.2.2)' $v
} else { Show 'LOI' 'JDK 8' "không thấy $jdk8 - chạy cai-dat-may-moi.ps1" }
$j = Get-Command java -ErrorAction SilentlyContinue
if ($j) { Show 'DAT' 'java trong PATH (chạy phần mềm)' (((& java -version 2>&1) | Select-Object -First 1) -join '') }
else { Show 'LOI' 'java trong PATH' 'không có' }

# ---------------------------------------------------------------- SQL Server
Head 'SQL Server'
$svc = Get-Service 'MSSQL$SQLEXPRESS' -ErrorAction SilentlyContinue
if ($svc -and $svc.Status -eq 'Running') { Show 'DAT' 'Dịch vụ MSSQL$SQLEXPRESS' ('đang chạy, kiểu khởi động: ' + $svc.StartType) }
elseif ($svc) { Show 'LOI' 'Dịch vụ MSSQL$SQLEXPRESS' ('đang ' + $svc.Status + ' - Start-Service ''MSSQL$SQLEXPRESS''') }
else { Show 'LOI' 'SQL Server Express' 'chưa cài (cai-dat-may-moi.ps1)' }
try {
    $c = New-Object Net.Sockets.TcpClient; $c.Connect('localhost', 1433); $c.Close()
    Show 'DAT' 'Cổng TCP 1433' 'mở trên localhost'
} catch { Show 'LOI' 'Cổng TCP 1433' 'không kết nối được (TCP/IP chưa bật hoặc cổng khác)' }

$cfgFile = Join-Path $host_dir 'app.local.properties'
$pwd_sa = Prop $cfgFile 'db.password'
$usr = Prop $cfgFile 'db.user'
if (-not $pwd_sa) {
    Show 'CANH BAO' 'Đăng nhập SQL' 'chưa có code\host\app.local.properties với db.password (bỏ qua kiểm tra đăng nhập)'
} else {
    try {
        $cn = New-Object System.Data.SqlClient.SqlConnection ("Server=localhost,1433;Database=master;User Id=$usr;Password=$pwd_sa;Encrypt=False;Connect Timeout=8")
        $cn.Open()
        $cmd = $cn.CreateCommand()
        $cmd.CommandText = "SELECT CAST(SERVERPROPERTY('ProductVersion') AS varchar(30)) + ' ' + CAST(SERVERPROPERTY('Edition') AS varchar(60))"
        Show 'DAT' "Đăng nhập '$usr'" $cmd.ExecuteScalar()
        $cmd.CommandText = "SELECT is_auto_close_on FROM sys.databases WHERE name = 'the_sinh_vien'"
        $ac = $cmd.ExecuteScalar()
        if ($null -eq $ac) { Show 'CANH BAO' 'Database the_sinh_vien' 'chưa có (phần mềm tự tạo ở lần chạy đầu)' }
        else {
            Show 'DAT' 'Database the_sinh_vien' 'có'
            if ($ac -eq 1) { Show 'CANH BAO' 'AUTO_CLOSE' 'đang BẬT (chậm khi mở lại); chạy phần mềm một lần là tự tắt' }
            else { Show 'DAT' 'AUTO_CLOSE' 'đã tắt' }
        }
        $cn.Close()
    } catch { Show 'LOI' "Đăng nhập '$usr'" ($_.Exception.Message -replace '\s+', ' ') }
}

# ---------------------------------------------------------------- Thẻ / PC/SC
Head 'Thẻ thông minh (PC/SC)'
$sc = Get-Service SCardSvr -ErrorAction SilentlyContinue
if ($sc -and $sc.Status -eq 'Running') { Show 'DAT' 'Dịch vụ Smart Card (SCardSvr)' ('đang chạy, kiểu khởi động: ' + $sc.StartType) }
else { Show 'LOI' 'Dịch vụ Smart Card (SCardSvr)' 'không chạy - Set-Service SCardSvr -StartupType Automatic; Start-Service SCardSvr' }
try {
    $rd = Get-PnpDevice -Class SmartCardReader -ErrorAction Stop | Where-Object { $_.FriendlyName -match 'JAVACOS' }
    if ($rd) { foreach ($r in $rd) { Show 'DAT' 'Đầu đọc ảo' ($r.FriendlyName + ' [' + $r.Status + ']') } }
    else { Show 'CANH BAO' 'Đầu đọc ảo JAVACOS' 'chưa thấy: cài JCKit (hoặc driver SdtPcsc) rồi chạy lại' }
} catch { Show 'CANH BAO' 'Đầu đọc ảo JAVACOS' 'không liệt kê được thiết bị Smart Card' }
$busy = @()
foreach ($p in 9025, 9550, 9035, 9551, 5000) {
    if (Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue) { $busy += $p }
}
if ($busy.Count) { Show 'DAT' 'JCIDE đang chạy thẻ ảo?' ('cổng đang nghe: ' + ($busy -join ', ')) }
else { Show 'CANH BAO' 'JCIDE chưa chạy thẻ ảo' 'cổng 9025/9550/9035/9551/5000 trống: cần Run trong JCIDE trước khi dùng card.mode=pcsc' }

# ---------------------------------------------------------------- JCKit / JCIDE
Head 'JavaCardKit / JCIDE'
$kit = 'C:\JavaCardKit'
$items = @(
  @('JCIDE.exe', "$kit\SDK\Tools\JCIDE\JCIDE.exe"),
  @('JCOSPanel.exe', "$kit\SDK\Tools\jcpanel\JCOSPanel.exe"),
  @('JDK cho JCIDE (liên kết tới JDK 8)', "$kit\JDK\bin\javac.exe"),
  @('JCDK: api.jar', "$kit\JCDK\lib\api.jar"),
  @('JCDK: converter.jar', "$kit\JCDK\lib\converter.jar"),
  @('JCDK: api_export_files', "$kit\JCDK\api_export_files\javacard\framework\javacard\framework.exp")
)
foreach ($i in $items) {
    if (Test-Path $i[1]) { Show 'DAT' $i[0] $i[1] } else { Show 'LOI' $i[0] ('thiếu ' + $i[1]) }
}
if (Test-Path "$kit\SDK\Tools\JCIDE\JCIDE.exe") {
    Show 'DAT' 'Phiên bản JCIDE' ((Get-Item "$kit\SDK\Tools\JCIDE\JCIDE.exe").VersionInfo.ProductVersion)
}
Show 'CANH BAO' 'PyApduTool' 'không thấy trong C:\JavaCardKit (bộ 1.0.5.39); gửi APDU tay bằng JCIDE hoặc khung Nhật ký APDU của phần mềm'

# ---------------------------------------------------------------- Phần mềm
Head 'Phần mềm máy tính (code\host)'
foreach ($lib in 'flatlaf-3.5.4.jar', 'jcardsim-2.2.2.jar', 'mssql-jdbc-12.8.1.jre8.jar', 'sqlite-jdbc-3.41.2.2.jar') {
    $f = Join-Path $host_dir "lib\$lib"
    if (Test-Path $f) { Show 'DAT' "lib\$lib" ('{0:N0} KB' -f ((Get-Item $f).Length / 1KB)) } else { Show 'LOI' "lib\$lib" 'thiếu - chạy host\tai-thu-vien.cmd' }
}
if (Test-Path $cfgFile) {
    $mode = Prop $cfgFile 'card.mode'
    Show 'DAT' 'app.local.properties' ("card.mode=$mode, db.user=$usr")
    if ($mode -eq 'jcardsim') { Show 'CANH BAO' 'Chế độ thẻ' 'đang jcardsim (thẻ giả lập trong phần mềm). Báo cáo cần pcsc sau khi JCIDE Run thẻ ảo' }
} else { Show 'CANH BAO' 'app.local.properties' 'chưa có, phần mềm dùng app.properties (card.mode=pcsc, cần mật khẩu SQL)' }
$nb = 'C:\Tools\netbeans-32\bin\netbeans64.exe'
if (Test-Path $nb) { Show 'DAT' 'Apache NetBeans 32' $nb } else { Show 'CANH BAO' 'Apache NetBeans' 'chưa cài (không bắt buộc: chay-phan-mem.cmd chạy không cần NetBeans)' }

# ---------------------------------------------------------------- Kiểm thử (tùy chọn)
if ($ChayKiemThu) {
    Head 'Chạy kiểm thử'
    $code = Join-Path $root 'code'
    $steps = @(
      @('SimTest + UiShots (jCardSim + SQLite)', 'run-test.cmd', $code),
      @('SimTest trên SQL Server thật', 'run-test-sqlserver.cmd', $code),
      @('Build CAP + bộ kiểm tra Oracle', 'build-cap.cmd', (Join-Path $code 'applet'))
    )
    foreach ($s in $steps) {
        $out = & cmd.exe /c "cd /d `"$($s[2])`" && .\$($s[1]) 2>&1"
        $last = ($out | Where-Object { $_ -match 'TẤT CẢ|completed with|Xong|ERROR|SAI ' } | Select-Object -Last 3) -join ' | '
        if ($LASTEXITCODE -eq 0) { Show 'DAT' $s[0] $last } else { Show 'LOI' $s[0] $last }
    }
}

Write-Host ''
Write-Host ('Tổng: {0} đạt, {1} cảnh báo, {2} lỗi' -f $script:ok, $script:warn, $script:fail) -ForegroundColor White
if ($script:fail -gt 0) { exit 1 } else { exit 0 }
