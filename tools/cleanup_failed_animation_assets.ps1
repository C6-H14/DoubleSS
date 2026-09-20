$ErrorActionPreference = 'Stop'

$root = [System.IO.Path]::GetFullPath('D:\StSmod\DoubleSS\art_candidates')
$rootPrefix = $root.TrimEnd('\') + '\'

$targets = @(
    'attack_sword_frame_animation_v1',
    'attack_sword_frame_animation_v2',
    'attack_sword_frame_animation_v3_video',
    'attack_sword_frame_animation_v4_clean',
    'attack_sword_frame_animation_v5_webclean',
    'attack_sword_frame_animation_v6_final39',
    'attack_sword_frame_animation_v7_edgeclean40fps',
    'attack_sword_frame_animation_v8_swordfixed40fps',
    'attack_sword_frame_animation_v9_final350_clean40fps',
    'attack_sword_frame_animation_v10_final30_hires40fps',
    'side_body_split_v1',
    'side_head_split_v1',
    'firefly_tests',
    'matting_tests\matanyone_attack_v1',
    'matting_tests\latest_magenta_inspection',
    'video_source\attack_sword_v2_final16_15fps',
    'video_source\attack_sword_v2_fourpoint_affine39_v1',
    'video_source\attack_sword_v2_fourpoint_affine39_v2',
    'video_source\attack_sword_v2_fourpoint_affine39_v3',
    'video_source\attack_sword_v2_fourpoint_affine39_v4',
    'video_source\attack_sword_v2_43frame_v1',
    'video_source\attack_sword_v2_step1_39frame_v1',
    'video_source\attack_sword_v2_aligned39_v1',
    'video_source\attack_sword_v2_22frame_v1',
    'video_source\attack_sword_v2_selected18',
    'video_source\attack_sword_v2_final39_edgeclean_v5'
)

$resolved = @()
foreach ($relative in $targets) {
    $candidate = [System.IO.Path]::GetFullPath((Join-Path $root $relative))
    if (-not $candidate.StartsWith($rootPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing target outside art_candidates: $candidate"
    }
    if (Test-Path -LiteralPath $candidate -PathType Container) {
        $resolved += $candidate
    }
}

$bytes = 0L
foreach ($directory in $resolved) {
    $bytes += (Get-ChildItem -LiteralPath $directory -Recurse -File -ErrorAction SilentlyContinue |
        Measure-Object -Property Length -Sum).Sum
}

# Give the new source a stable project-local name before cleaning anything.
$newVideo = Get-ChildItem -LiteralPath (Join-Path $root 'video_source') -File -Filter '*.mp4' |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
$castDir = Join-Path $root 'video_source\cast_v1'
$stableVideo = Join-Path $castDir 'cast_attack_1_source.mp4'
if ($null -ne $newVideo -and -not (Test-Path -LiteralPath $stableVideo)) {
    Copy-Item -LiteralPath $newVideo.FullName -Destination $stableVideo
}

foreach ($directory in $resolved) {
    Remove-Item -LiteralPath $directory -Recurse -Force
    Write-Output "removed: $directory"
}

Write-Output ("removed_directories={0}" -f $resolved.Count)
Write-Output ("freed_mb={0:N1}" -f ($bytes / 1MB))
Write-Output "preserved_current_attack=matting_tests\matanyone_attack_pink_v1"
Write-Output "preserved_master=video_source\attack_sword_v2_final39_swordfixed_v6"
Write-Output "stable_cast_video=$stableVideo"
