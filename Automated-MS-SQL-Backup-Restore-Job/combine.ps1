# Get current directory
$currentDir = Get-Location
$outputFile = "combined_files.txt"
$extensions = @("*.tf", "*.gvy", "*.ps1", "*.tfvars", "*.py")

if (Test-Path $outputFile) {
    Remove-Item $outputFile
}

$files = Get-ChildItem -Path $currentDir -Recurse -File -Include $extensions
$totalFiles = $files.Count
$counter = 0

# Create a string builder to collect all content
$combinedContent = [System.Text.StringBuilder]::new()

foreach ($file in $files) {
    $counter++
    $percentComplete = ($counter / $totalFiles) * 100
    Write-Progress -Activity "Processing Files" -Status "Processing $($file.Name) ($counter of $totalFiles)" -PercentComplete $percentComplete
    
    $relativePath = $file.FullName.Substring($currentDir.Path.Length + 1)
    [void]$combinedContent.AppendLine($relativePath)
    [void]$combinedContent.AppendLine("=" * $relativePath.Length)
    
    try {
        $content = Get-Content -Path $file.FullName -Raw
        [void]$combinedContent.AppendLine($content)
    }
    catch {
        $errorMsg = "[Error reading file: $($_.Exception.Message)]"
        [void]$combinedContent.AppendLine($errorMsg)
    }
    
    [void]$combinedContent.AppendLine()
    [void]$combinedContent.AppendLine()
}

# Write all content to file at once
try {
    $combinedContent.ToString() | Out-File -FilePath $outputFile -Encoding UTF8
    Write-Host "Combined file created: $outputFile" -ForegroundColor Green
    Write-Host "Total files processed: $totalFiles" -ForegroundColor Green
}
catch {
    Write-Host "Error writing to output file: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Progress -Activity "Processing Files" -Completed