param(
    [switch]$List,
    [switch]$DiscardUncommitted,
    [string]$Target = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if ($List) {
    Write-Host "=== 15 commit gan nhat ===" -ForegroundColor Cyan
    git log --oneline -n 15
    Write-Host "`n=== Cac tag da danh dau ===" -ForegroundColor Cyan
    git tag -l
    exit 0
}

if ($DiscardUncommitted) {
    Write-Host "Trang thai hien tai:" -ForegroundColor Yellow
    git status --short
    $confirm = Read-Host "Ban co chac muon huy TOAN BO thay doi chua commit? (y/N)"
    if ($confirm -ne "y" -and $confirm -ne "Y") {
        Write-Host "Da huy thao tac." -ForegroundColor Yellow
        exit 0
    }
    git restore --staged . 2>$null
    git restore .
    git clean -fd
    Write-Host "Da dua code ve trang thai sach cua commit hien tai." -ForegroundColor Green
    exit 0
}

if (-not $Target) {
    Write-Host "Cach dung:" -ForegroundColor Yellow
    Write-Host "  .\scripts\rollback.ps1 -List"
    Write-Host "  .\scripts\rollback.ps1 -DiscardUncommitted"
    Write-Host "  .\scripts\rollback.ps1 -Target <commit-id-hoac-tag>"
    exit 1
}

# Kiem tra target hop le
git rev-parse --verify "$Target^{commit}" > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Khong tim thay commit/tag '$Target'. Dung -List de xem danh sach." -ForegroundColor Red
    exit 1
}

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupBranch = "backup/before-rollback-$stamp"

# Luu thay doi chua commit vao backup branch neu co
$dirty = git status --porcelain
if ($dirty) {
    git branch $backupBranch
    git checkout $backupBranch
    git add -A
    git commit -m "backup: auto save before rollback to $Target"
    git checkout -
} else {
    git branch $backupBranch
}

Write-Host "Da tao nhanh du phong '$backupBranch'." -ForegroundColor Cyan
$confirm = Read-Host "Xac nhan quay nhanh hien tai ve '$Target'? (y/N)"
if ($confirm -ne "y" -and $confirm -ne "Y") {
    Write-Host "Da huy thao tac rollback." -ForegroundColor Yellow
    exit 0
}

git reset --hard $Target
git clean -fd
Write-Host "Da quay ve moc '$Target'. Neu can lay lai code cu, xem nhanh '$backupBranch'." -ForegroundColor Green
