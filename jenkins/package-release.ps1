param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^v[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta|rc)\.[0-9]+)?$')]
    [string]$Tag
)

$ErrorActionPreference = 'Stop'

$sourceDirectory = Join-Path $PSScriptRoot '..\ideaflow-knime\IDEAFlow.update\target'
$sourceZip = Get-ChildItem -LiteralPath $sourceDirectory -Filter 'org.ideaflow.update-*.zip' -File |
    Select-Object -First 1

if ($null -eq $sourceZip) {
    throw "The Maven build did not produce an update-site ZIP in $sourceDirectory"
}

$distributionDirectory = Join-Path $PSScriptRoot '..\dist'
New-Item -ItemType Directory -Path $distributionDirectory -Force | Out-Null

$releaseName = "IdeaFlow-$Tag-update-site.zip"
$releasePath = Join-Path $distributionDirectory $releaseName
Copy-Item -LiteralPath $sourceZip.FullName -Destination $releasePath -Force

$hash = (Get-FileHash -LiteralPath $releasePath -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath "$releasePath.sha256" -Value "$hash  $releaseName" -Encoding ascii
