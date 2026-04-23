$ErrorActionPreference = "Stop"
$base = "http://localhost:8081"
$datasetDir = "d:\HuaweiMoveData\Users\hbj\Desktop\EV-Battery\dataset"

function Login($username, $password) {
  $body = @{ username = $username; password = $password } | ConvertTo-Json
  $res = Invoke-RestMethod -Uri "${base}/api/user/login" -Method Post -ContentType "application/json" -Body $body
  return $res.data.token
}

$token = Login "seller01" "123456"
$headers = @{ Authorization = "Bearer $token" }
$csvFiles = Get-ChildItem -Path $datasetDir -Filter "*.csv" | Select-Object -First 20
if (-not $csvFiles.Count) {
  throw "No csv files in dataset folder"
}

function First-Value($row, $keys, $defaultValue) {
  foreach ($key in $keys) {
    $match = $row.PSObject.Properties | Where-Object { $_.Name.ToLower().Replace("_","").Contains($key) } | Select-Object -First 1
    if ($match -and $match.Value -ne $null -and "$($match.Value)" -ne "") {
      return [double]$match.Value
    }
  }
  return $defaultValue
}

$created = 0
foreach ($file in $csvFiles) {
  try {
    $row = Import-Csv -Path $file.FullName | Select-Object -First 1
    if ($null -eq $row) { continue }
    $voltage = First-Value $row @("voltage","cellvoltage","v") 365
    $cap = First-Value $row @("capacityretention","soh","capacity") 80
    $ir = First-Value $row @("internalresistance","resistance","ir") 1.2
    $cycle = [int](First-Value $row @("cyclecount","cycle","cycles") 600)
    $temp = First-Value $row @("temperature","temp","avgtemp") 28
    $manualBody = @{
      sourceType = "dataset-import"
      remark = $file.Name
      voltage = [math]::Round($voltage, 2)
      capacityRetentionRate = [math]::Round($cap, 2)
      internalResistanceRatio = [math]::Round($ir, 2)
      cycleCount = $cycle
      avgTemperature = [math]::Round($temp, 2)
      status = "PENDING_ASSESSMENT"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "${base}/api/battery/manual" -Method Post -Headers $headers -ContentType "application/json" -Body $manualBody | Out-Null
    $created++
  } catch {
    Write-Host "Skip file: $($file.Name), reason: parse or import failed"
  }
}

$list = Invoke-RestMethod -Uri "${base}/api/battery/list?page=1&size=100" -Method Get -Headers $headers
$batteryIds = @($list.data.records | Where-Object { $_.sourceType -eq "dataset-import" } | Select-Object -First 20 | ForEach-Object { $_.id })
if (-not $batteryIds.Count) {
  throw "No dataset batteries to assess"
}

$taskBody = @{ batteryIds = $batteryIds; useML = $true } | ConvertTo-Json
$task = Invoke-RestMethod -Uri "${base}/api/assessment/batch/trigger" -Method Post -Headers $headers -ContentType "application/json" -Body $taskBody
$taskId = $task.data.taskId

for ($i = 0; $i -lt 60; $i++) {
  $status = Invoke-RestMethod -Uri "${base}/api/assessment/batch/task/$taskId" -Method Get -Headers $headers
  if ($status.data.finished) {
    Write-Host "Batch done: success=$($status.data.successCount), fail=$($status.data.failCount)"
    break
  }
  Start-Sleep -Seconds 1
}

$firstId = $batteryIds[0]
$latest = Invoke-RestMethod -Uri "${base}/api/assessment/battery/$firstId/latest" -Method Get -Headers $headers
Write-Host ("Sample: batteryId={0}, healthScore={1}, llmSummary={2}" -f $firstId, $latest.data.healthScore, $latest.data.llmSummary)
Write-Host ("Created records: {0}" -f $created)
