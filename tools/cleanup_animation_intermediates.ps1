$ErrorActionPreference = 'Stop'

$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$art = (Resolve-Path (Join-Path $project 'art_candidates')).Path
$video = (Resolve-Path (Join-Path $art 'video_source')).Path
$report = Join-Path $project 'animation_cleanup_manifest_20260924.txt'

function Assert-SafePath([string]$Path, [string]$Root) {
    $full = [System.IO.Path]::GetFullPath($Path)
    $rootFull = [System.IO.Path]::GetFullPath($Root).TrimEnd('\') + '\'
    if (-not $full.StartsWith($rootFull, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Unsafe cleanup target outside $Root : $full"
    }
    return $full
}

$keepArt = @(
    'character_runtime_v32_idle_shoes',
    'death_corpse_static_v1',
    'video_source'
)

$keepVideo = @{
    'attack_sword_v2_final39_swordfixed_v6' = @('.')
    'cast_v1' = @('final_v4_edgeclean_37f_50fps')
    'new_animation_candidates_v1' = @(
        'attack_heavy\pink_frames',
        'cast_debuff\pink_frames',
        'cast_buff_transition_test_v6\pink_frames'
    )
    'cast_attack_2_final_cut' = @('alpha_frames_115s_30fps_sword_completed_v2')
    'cast_prayer' = @('preview_v9_matanyone_grounded')
    'command_ally' = @('final_v3_degreen_noholes')
    'cast_special' = @('final_v4_pixelwise')
    'hurt' = @('final_v5_42f_defringed')
    'idle' = @('short_blink_final_v47_shoes_repaired')
}

$delete = [System.Collections.Generic.List[string]]::new()

# Remove all obsolete top-level animation candidates.
Get-ChildItem -LiteralPath $art -Force | ForEach-Object {
    if ($_.PSIsContainer -and $keepArt -notcontains $_.Name) {
        $delete.Add($_.FullName)
    }
}

# Loose source videos/GIFs have already been distilled into final frame sets.
Get-ChildItem -LiteralPath $video -File -Force | ForEach-Object { $delete.Add($_.FullName) }

# Remove obsolete top-level video-source groups.
Get-ChildItem -LiteralPath $video -Directory -Force | ForEach-Object {
    if (-not $keepVideo.ContainsKey($_.Name)) {
        $delete.Add($_.FullName)
    }
}

# Inside each retained group, keep only the explicit final paths and their
# ancestors. Everything else is an intermediate or failed version.
foreach ($groupName in $keepVideo.Keys) {
    $group = Join-Path $video $groupName
    if (-not (Test-Path -LiteralPath $group)) { continue }
    $relativeKeeps = $keepVideo[$groupName]
    if ($relativeKeeps -contains '.') { continue }
    $absoluteKeeps = @($relativeKeeps | ForEach-Object {
        [System.IO.Path]::GetFullPath((Join-Path $group $_)).TrimEnd('\')
    })
    Get-ChildItem -LiteralPath $group -Force | ForEach-Object {
        $candidate = [System.IO.Path]::GetFullPath($_.FullName).TrimEnd('\')
        $needed = $false
        foreach ($keep in $absoluteKeeps) {
            if ($keep.Equals($candidate, [System.StringComparison]::OrdinalIgnoreCase) -or
                $keep.StartsWith($candidate + '\', [System.StringComparison]::OrdinalIgnoreCase)) {
                $needed = $true
                break
            }
        }
        if (-not $needed) { $delete.Add($_.FullName) }
    }
    # Prune siblings below retained ancestor directories, while considering all
    # retained paths together (one retained branch must not delete another).
    $ancestors = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase)
    foreach ($keep in $absoluteKeeps) {
        $current = Split-Path -Parent $keep
        while ($current.StartsWith($group, [System.StringComparison]::OrdinalIgnoreCase)) {
            [void]$ancestors.Add($current)
            if ($current.Equals($group, [System.StringComparison]::OrdinalIgnoreCase)) { break }
            $current = Split-Path -Parent $current
        }
    }
    foreach ($parent in $ancestors) {
        Get-ChildItem -LiteralPath $parent -Force | ForEach-Object {
            $candidate = [System.IO.Path]::GetFullPath($_.FullName).TrimEnd('\')
            $needed = $false
            foreach ($keep in $absoluteKeeps) {
                if ($keep.Equals($candidate, [System.StringComparison]::OrdinalIgnoreCase) -or
                    $keep.StartsWith($candidate + '\', [System.StringComparison]::OrdinalIgnoreCase)) {
                    $needed = $true
                    break
                }
            }
            if (-not $needed) { $delete.Add($_.FullName) }
        }
    }
}

$delete = @($delete | Sort-Object -Unique)
$safeTargets = @()
$totalBytes = 0L
foreach ($path in $delete) {
    if (-not (Test-Path -LiteralPath $path)) { continue }
    $safe = if ([System.IO.Path]::GetFullPath($path).StartsWith($video, [System.StringComparison]::OrdinalIgnoreCase)) {
        Assert-SafePath $path $video
    } else {
        Assert-SafePath $path $art
    }
    $safeTargets += $safe
    if (Test-Path -LiteralPath $safe -PathType Container) {
        $totalBytes += [long]((Get-ChildItem -LiteralPath $safe -Recurse -File -Force -ErrorAction SilentlyContinue |
            Measure-Object Length -Sum).Sum)
    } else {
        $totalBytes += (Get-Item -LiteralPath $safe).Length
    }
}

$lines = @(
    "Animation cleanup 2026-09-24",
    "Project: $project",
    "Targets: $($safeTargets.Count)",
    "Estimated bytes: $totalBytes",
    "Estimated GiB: $([math]::Round($totalBytes / 1GB, 3))",
    '',
    'KEPT:',
    (Join-Path $art 'character_runtime_v32_idle_shoes'),
    (Join-Path $art 'death_corpse_static_v1')
)
foreach ($groupName in ($keepVideo.Keys | Sort-Object)) {
    foreach ($relative in $keepVideo[$groupName]) {
        $lines += Join-Path (Join-Path $video $groupName) $relative
    }
}
$lines += ''
$lines += 'DELETED:'
$lines += $safeTargets
Set-Content -LiteralPath $report -Value $lines -Encoding UTF8

foreach ($target in $safeTargets) {
    Remove-Item -LiteralPath $target -Recurse -Force
}

$cache = Join-Path $project 'tools\__pycache__'
if (Test-Path -LiteralPath $cache) {
    $safeCache = Assert-SafePath $cache (Join-Path $project 'tools')
    Remove-Item -LiteralPath $safeCache -Recurse -Force
}

Write-Output "Deleted $($safeTargets.Count) targets; estimated $([math]::Round($totalBytes / 1GB, 3)) GiB."
Write-Output "Manifest: $report"
