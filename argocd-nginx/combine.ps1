# Get current directory
$currentDir = Get-Location
$outputFile = "combined_files.txt"
$extensions = @("*.yaml", "*.txt"  )

if (Test-Path $outputFile) {
    Remove-Item $outputFile
}

$files = Get-ChildItem -Path $currentDir -Recurse -File -Include $extensions
$totalFiles = $files.Count
$counter = 0

foreach ($file in $files) {
    $counter++
    $percentComplete = ($counter / $totalFiles) * 100
    Write-Progress -Activity "Processing Files" -Status "Processing $($file.Name) ($counter of $totalFiles)" -PercentComplete $percentComplete
    
    $relativePath = $file.FullName.Substring($currentDir.Path.Length + 1)
    Add-Content -Path $outputFile -Value "$relativePath"
    
    try {
        $content = Get-Content -Path $file.FullName -Raw
        Add-Content -Path $outputFile -Value $content
    }
    catch {
        Add-Content -Path $outputFile -Value "[Error reading file: $($_.Exception.Message)]"
    }
    
    Add-Content -Path $outputFile -Value "`n"
}

Write-Progress -Activity "Processing Files" -Completed
Write-Host "Combined file created: $outputFile" -ForegroundColor Green
Write-Host "Total files processed: $totalFiles" -ForegroundColor Green