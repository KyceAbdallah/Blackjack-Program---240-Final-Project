# Blackjack Saloon

Blackjack Saloon is a Java desktop blackjack game with a cinematic saloon table, rail patrons with their own betting styles, persistent progression, and a quick-draw accusation system layered on top of the card play.

## What Changed

- Kept the existing visuals and layout direction intact while moving the custom table renderer into its own class.
- Added a working duel flow so accusations now lead somewhere instead of stopping at placeholder text.
- Improved save handling by storing data in a user save directory and persisting current streak and suspicion as well.
- Added repeatable local build and test scripts for the JDK already installed on the machine.
- Added automated regression tests for blackjack payout, pushes, doubles, splits, duel flow, and save/load state.

## Run It

From the project root:

```powershell
.\scripts\build.ps1
java -cp build\main Main
```

## Run Tests

```powershell
.\scripts\test.ps1
```

## Optional Gradle Setup

The repository now includes `build.gradle` and `settings.gradle` for a standard Java project layout. If Gradle is installed on your machine later, you can use that setup too.

## Save Location

Runtime save data is stored under:

```text
%USERPROFILE%\.blackjack-saloon\blackjack-save.properties
```

That keeps local save progress out of the repository and separate from source files.
