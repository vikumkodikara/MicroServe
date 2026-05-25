$ErrorActionPreference = "Stop"
Set-Location "C:\Users\MSI\AndroidStudioProjects\MicroServe"

function Commit-Message($msg) {
    git add -A
    git commit -m $msg
}

# Commit 2: nav base bar
@'
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/navContainer"
    android:layout_width="match_parent"
    android:layout_height="115dp">

    <ImageView
        android:id="@+id/navBase"
        android:layout_width="match_parent"
        android:layout_height="70dp"
        android:contentDescription="@null"
        android:scaleType="fitXY"
        android:src="@drawable/rectanglenav"
        app:layout_constraintBottom_toBottomOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
'@ | Set-Content -Path "app/src/main/res/layout/view_user_bottom_nav.xml" -NoNewline
Commit-Message "feat: add purple base bar layer to shared bottom nav"

# Commit 3: bubble layers
@'
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/navContainer"
    android:layout_width="match_parent"
    android:layout_height="115dp">

    <ImageView
        android:id="@+id/navBase"
        android:layout_width="match_parent"
        android:layout_height="70dp"
        android:contentDescription="@null"
        android:scaleType="fitXY"
        android:src="@drawable/rectanglenav"
        app:layout_constraintBottom_toBottomOf="parent" />

    <ImageView
        android:id="@+id/navSubtract"
        android:layout_width="120dp"
        android:layout_height="80dp"
        android:contentDescription="@null"
        android:scaleType="fitCenter"
        android:src="@drawable/subtractnav"
        android:translationY="-15dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintHorizontal_bias="0.75"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="@id/navBase"
        app:layout_constraintVertical_bias="0.0" />

    <ImageView
        android:id="@+id/activeEllipse"
        android:layout_width="60dp"
        android:layout_height="60dp"
        android:contentDescription="@null"
        android:src="@drawable/ellipsenav"
        android:translationY="-30dp"
        app:layout_constraintBottom_toBottomOf="@id/navSubtract"
        app:layout_constraintEnd_toEndOf="@id/navSubtract"
        app:layout_constraintStart_toStartOf="@id/navSubtract"
        app:layout_constraintTop_toTopOf="@id/navSubtract" />

    <ImageView
        android:id="@+id/navActiveIcon"
        android:layout_width="38dp"
        android:layout_height="38dp"
        android:contentDescription="@null"
        android:elevation="10dp"
        android:src="@drawable/navpost"
        android:translationY="-30dp"
        app:layout_constraintBottom_toBottomOf="@id/activeEllipse"
        app:layout_constraintEnd_toEndOf="@id/activeEllipse"
        app:layout_constraintStart_toStartOf="@id/activeEllipse"
        app:layout_constraintTop_toTopOf="@id/activeEllipse" />

</androidx.constraintlayout.widget.ConstraintLayout>
'@ | Set-Content -Path "app/src/main/res/layout/view_user_bottom_nav.xml" -NoNewline
Commit-Message "feat: add floating bubble layers to shared user bottom nav"

# Read final layout from stash for remaining tab commits - use incremental append
$finalLayout = git show "stash@{0}:app/src/main/res/layout/view_user_bottom_nav.xml" 2>$null
if (-not $finalLayout) {
    git stash show -p stash@{0} -- app/src/main/res/layout/view_user_bottom_nav.xml | Out-Null
    $finalLayout = Get-Content "app/src/main/res/layout/view_user_bottom_nav.xml" -Raw
}

# Restore from stash pop at end - for now extract final files from stash
git checkout stash@{0} -- app/src/main/res/layout/view_user_bottom_nav.xml 2>$null
if ($LASTEXITCODE -ne 0) {
    git stash show -p | Out-Null
}

Write-Host "Script partial - continuing manually"
