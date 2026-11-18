param(
    [Parameter(Mandatory = $true)]
    [string]$Message,

    [string]$Tag = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$status = git status --porcelain
if (-not $status) {
    Write-Host "Khong co thay doi nao de commit." -ForegroundColor Yellow
    if ($Tag) {
        git tag -f $Tag
        Write-Host "Da gan tag '$Tag' vao commit hien tai." -ForegroundColor Green
    }
    exit 0
}

git add -A
git commit -m $Message

if ($Tag) {
    git tag -f $Tag
    Write-Host "Da luu moc thanh cong voi tag '$Tag'." -ForegroundColor Green
} else {
    $shortHash = git rev-parse --short HEAD
    Write-Host "Da luu moc thanh cong tai commit $shortHash." -ForegroundColor Green
}
