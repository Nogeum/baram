$ErrorActionPreference = 'Stop'
$portalDeckRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\docs\presentation'))
$portalPreviewPath = Join-Path $portalDeckRoot 'preview'
New-Item -ItemType Directory -Path $portalPreviewPath -Force | Out-Null
$portalPowerPointWasRunning = @(Get-Process POWERPNT -ErrorAction SilentlyContinue).Count -gt 0
$portalPowerPoint = New-Object -ComObject PowerPoint.Application
$portalPresentation = $null
try {
    $portalPresentation = $portalPowerPoint.Presentations.Open((Join-Path $portalDeckRoot 'portal-team-presentation.pptx'), -1, 0, 0)
    $portalPresentation.SaveAs((Join-Path $portalDeckRoot 'portal-team-presentation.pdf'), 32)
    $portalPresentation.Export($portalPreviewPath, 'PNG', 1600, 900)
    $portalOverflow = @()
    foreach ($portalSlide in $portalPresentation.Slides) {
        foreach ($portalShape in $portalSlide.Shapes) {
            if ($portalShape.HasTextFrame -eq -1 -and $portalShape.TextFrame.HasText -eq -1) {
                if ($portalShape.TextFrame2.TextRange.BoundHeight -gt $portalShape.Height + 4) {
                    $portalOverflow += [pscustomobject]@{slide=$portalSlide.SlideIndex; shape=$portalShape.Name; height=$portalShape.Height; textHeight=$portalShape.TextFrame2.TextRange.BoundHeight}
                }
            }
        }
    }
    @{slides=$portalPresentation.Slides.Count; textOverflows=$portalOverflow} | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $portalDeckRoot 'validation.json') -Encoding UTF8
    Write-Output ('Exported PDF and previews: ' + $portalPresentation.Slides.Count + ' slides; text overflow candidates=' + $portalOverflow.Count)
} finally {
    if ($null -ne $portalPresentation) { $portalPresentation.Close() }
    if (-not $portalPowerPointWasRunning) { $portalPowerPoint.Quit() }
    [Runtime.InteropServices.Marshal]::ReleaseComObject($portalPowerPoint) | Out-Null
}
