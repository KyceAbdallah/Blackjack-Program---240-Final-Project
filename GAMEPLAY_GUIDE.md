# Blackjack Saloon Gameplay Guide

This document explains how **Blackjack Saloon** works based on the current code in this repository. It covers the main blackjack rules, the saloon-specific systems, the rail patrons, the duel mechanic, scoring, and save behavior.

## 1. Game Overview

Blackjack Saloon is a Java desktop blackjack game with a western saloon presentation layered on top of a standard dealer-versus-player blackjack loop.

At its core, the player:

- starts with a bankroll of **$500**
- places a bet with a **$10 minimum**
- plays one or more hands against the dealer
- can use standard blackjack actions like **Hit**, **Stand**, **Double**, and **Split**
- can also use a special **Cheat** action that improves the hand but raises suspicion
- may get pulled into a **quick-draw duel** if the table decides the cheating was too obvious

The game also includes four simulated rail patrons who bet, play, and resolve their own hands after the player's round ends. They do not directly change the player's payout, but they contribute to the atmosphere, pot display, and event narration.

## 2. Objective

The main goal is to grow your bankroll by beating the dealer without going over 21.

Longer-term progression is tracked through:

- total **wins**
- total **losses**
- current **win streak**
- **best streak**
- current **suspicion**

There is no final campaign ending in the current code. The game is designed as a looping table experience with persistent progress.

## 3. Deck, Card Values, and Dealer Rules

### Deck / shoe

- The game uses a **6-deck shoe** during normal play.
- When the shoe drops below **15 cards**, it automatically refills and reshuffles.

### Card values

- Number cards are worth face value.
- `10`, `J`, `Q`, and `K` are all worth **10**.
- `A` starts as **11**, but automatically counts as **1** if needed to avoid busting.

### Dealer behavior

- The dealer follows a simple rule: **hit until reaching 17 or more**.
- During an active round, only the dealer's up-card is shown in full.
- Once the round resolves, the full dealer value is revealed.

## 4. How a Normal Round Works

Each round follows this flow:

1. The player enters a wager and presses **Deal In**.
2. A fresh player hand and dealer hand are created.
3. Each rail patron opens a new side round and places a bet.
4. Two dealing passes happen:
   - each rail patron gets a card
   - the player gets a card
   - the dealer gets a card
5. After two passes, everyone has an opening hand.
6. The player plays first.
7. After the player finishes, the dealer plays out the dealer hand.
8. The player's hand or hands are resolved.
9. If no duel interrupts the flow, each rail patron then plays and settles their hand.
10. The round finalizes, suspicion cools slightly, and the next hand becomes available.

If either the player or dealer has a natural blackjack immediately after the opening deal, the round resolves right away.

## 5. Player Actions

### Deal In

- Starts a round using the entered bet.
- Cannot be used while loading, during an active hand, or during a duel.
- Requires:
  - bet >= **$10**
  - bet <= current bankroll

### Hit / Take Card

- Deals one additional card to the active hand.
- If the hand busts, reaches 21, or was previously doubled down, the game advances automatically.

### Stand / Hold

- Locks the active hand and moves to the next hand or to the dealer.

### Double

- Available only when:
  - a round is active
  - the current hand has exactly **2 cards**
  - the player has enough bankroll to match that hand's current bet
- The hand's bet is doubled immediately.
- One final card is dealt.
- The hand is then considered finished.

### Split

- Available only when:
  - a round is active
  - the current hand has exactly **2 cards**
  - both cards have the same rank
  - the player can afford the extra matching bet
- The pair becomes two separate hands.
- A second equal bet is deducted from bankroll.
- Each split hand receives one replacement card.
- The player then plays through the hands one at a time.

### Cheat

- Available during an active player round as long as suspicion is below **100**.
- The game searches all possible one-card swaps in the current hand and picks the replacement that gets closest to **21**.
- This means Cheat is not random; it chooses the best legal one-card improvement according to the current hand value logic.
- Cheat can improve weak hands and can even rescue some busted-looking situations.

Cost of cheating:

- each cheat adds suspicion equal to:
  - **14 + opponent suspicion sensitivity**

Other notes:

- The game only tracks whether you cheated at least once in the round, not how many times.
- Cheat does not directly cost bankroll.
- Cheat can indirectly cost a lot if it triggers a duel and you lose.

## 6. Multi-Hand Flow

If the player splits, the game keeps an `activeHandIndex` and walks through the hands from left to right.

A hand is treated as finished once any of the following happens:

- it busts
- it stands
- it doubles down
- it reaches 21

Only after all player hands are done does the dealer take cards and the game resolve outcomes.

## 7. Payout Rules

The game removes the bet from bankroll when the round begins, then pays money back during resolution.

### Standard win

- payout: **2x the hand bet**
- effect: original stake is effectively returned plus one bet of profit

Example:

- bet $50
- bankroll drops by $50 when the hand starts
- on a win, bankroll gains $100
- net result: **+$50**

### Natural blackjack

- payout: **2.5x the hand bet**
- effect: standard **3:2 blackjack payout**

Example:

- bet $50
- bankroll gains $125 at resolution
- net result: **+$75**

### Push

- payout: **1x the hand bet**
- effect: original stake is returned, with no profit or loss

### Loss

- payout: **$0**
- the original bet is already gone

### Bust

- handled as a loss

### Double-down resolution

- because the hand bet was doubled before settlement, the same win/push/loss rules apply against the larger amount

## 8. Streaks, Record, and Bankroll Safety Net

### Wins and losses

- each winning hand increments the game's total win counter
- each losing hand increments the total loss counter
- pushes do not affect wins or losses

### Streaks

- every win increments the current streak
- every loss resets the current streak to **0**
- pushes leave the streak unchanged
- the best streak persists across sessions

Because wins are counted per resolved hand, split hands can affect streak progress one hand at a time.

### Bankroll bailout

If the player's bankroll falls below the $10 minimum after a round or duel:

- the bartender automatically fronts the player **$150**

This prevents a hard game-over state and keeps the loop going.

## 9. Suspicion System

Suspicion is a persistent stat from **0 to 100**.

It goes up when the player cheats and comes down after rounds or successful duels.

### Suspicion increase

Each cheat adds:

- **14 + current opponent's suspicion sensitivity**

The current opponents have these sensitivities:

- **Dusty Mae**: 6
- **The Drunk Miner**: 4
- **The Shady Gambler**: 9
- **Sheriff Boone**: 7

So a single cheat adds between **18 and 23 suspicion** depending on who is across the table.

### Suspicion cooling

At normal round finalization:

- lose at least one hand in the round: suspicion drops by **8**
- otherwise: suspicion drops by **4**

After winning a duel:

- suspicion drops by **40**

After losing a duel:

- suspicion resets to **0**

## 10. Duel / Accusation System

This is the main saloon-specific risk-reward mechanic.

### When a duel can start

A duel check only happens if:

- the player cheated during the round, and
- suspicion is at least **35**

If those conditions are met, the game rolls an accusation chance.

### Accusation chance formula

Base chance:

- `suspicion`
- `+ opponent suspicion sensitivity * 7`
- `+ opponent aggression * 8`

If the player's current streak is at least **3**, add:

- `+20`

The final chance is capped at **95%**.

If the random roll succeeds, the opponent accuses the player and the duel sequence begins instead of normal round cleanup.

### Opponent stats that matter

Each named opponent has:

- a suspicion sensitivity
- an aggression value
- a duel difficulty value

Current opponents:

| Opponent | Suspicion Sensitivity | Aggression | Duel Difficulty |
| --- | ---: | ---: | ---: |
| Dusty Mae | 6 | 1 | 180 |
| The Drunk Miner | 4 | 3 | 260 |
| The Shady Gambler | 9 | 2 | 170 |
| Sheriff Boone | 7 | 2 | 145 |

### Duel flow

1. Round summary is shown.
2. The opponent calls cheat.
3. The game tells the player to wait for the draw cue.
4. Once armed, the player may press **Draw** or **SPACE**.
5. Drawing too early is an automatic loss.
6. Drawing after the cue compares player reaction time against the opponent window.

### Reaction rule

Player reaction time:

- current time - time when the draw opened

Opponent reaction window:

- `max(120, duelDifficulty + random(0..120) - aggression * 12)`

The player wins if:

- `reactionTime <= opponentWindow`

### Duel rewards and penalties

If the player wins:

- bankroll gain = `60 + aggression * 15`
- total wins increment
- streak increments
- suspicion cools by **40**
- a new opponent sits down

If the player loses:

- penalty = the smaller of:
  - current bankroll
  - `max(75, bankroll / 5)`
- total losses increment
- streak resets
- suspicion resets to **0**
- a new opponent sits down

In practice, the duel is both a punishment system and a second chance system: cheating can save a hand, but it can also turn the round into a reaction-time gamble.

## 11. Rail Patrons

Four AI rail patrons sit around the table:

- **Lucky Len**: bold
- **Velvet Ruth**: conservative
- **Tin Cup Nora**: opportunist
- **Doc Mercer**: balanced

Each patron has:

- an opening stack
- a preferred bet
- a table minimum
- a play style

At the start of each round, each patron:

1. refills if broke enough to fall below the table minimum
2. chooses a bet using their style
3. receives cards during the same initial dealing passes as the player and dealer

After the player's hand resolves, each patron:

1. optionally presses the bet
2. may cheat according to style probability
3. may double down if their style says so and they can afford it
4. otherwise hits until a style-based stand threshold is reached
5. settles against the dealer

### Rail style personality summary

**Conservative**

- smaller bet range
- doubles mostly on strong 10 or 11 vs favorable dealer cards
- stands earlier
- very low cheat chance

**Balanced**

- medium bet range
- practical doubles on 9 to 11
- moderate press rate
- modest cheat chance

**Bold**

- biggest betting pressure
- loosest standing threshold
- most aggressive pressing
- higher cheat chance

**Opportunist**

- variable betting
- middling stand threshold
- strongest single press amount
- highest cheat chance of the four

Rail patrons do not directly alter the player's bankroll. Their main gameplay role is:

- narrative texture
- visual table activity
- pot contribution in the UI
- reinforcing the feeling that the saloon is an active room

## 12. Opponent Rotation

The opponent across the table changes under two circumstances:

- after any duel resolves
- after a normal round finalizes while the player has a streak of **3 or more**

That means hot streaks naturally cycle the people facing the player, even without a duel.

## 13. Controls and UI Notes

### Main actions

- **Deal In**
- **Take Card**
- **Hold**
- **Double**
- **Split**
- **Cheat**
- **Draw**
- **Reset**
- **How To Play**

### Keyboard shortcuts

- `Enter` = deal
- `H` = hit
- `S` = stand
- `D` = double
- `P` = split
- `C` = cheat
- `SPACE` = advance story text and also handle duel draw timing

### Important display notes

- The center table shows the shared pot, including the player and rail bets.
- The story / event panel communicates warnings, duel prompts, and table flavor.
- The active player hand is highlighted when multiple hands exist.

## 14. Saving and Resetting

The game persists progress to:

`%USERPROFILE%\.blackjack-saloon\blackjack-save.properties`

Saved values:

- bankroll
- best streak
- wins
- losses
- current streak
- suspicion

The **Reset** action restores:

- bankroll to **$500**
- streak to **0**
- best streak to **0**
- suspicion to **0**
- wins to **0**
- losses to **0**

It also resets rail patron session state and selects a new random opponent.

## 15. What Is and Is Not Implemented

Implemented:

- normal blackjack scoring
- blackjack payout at 3:2
- hit / stand / double / split
- multi-hand resolution after split
- suspicion and cheating
- duel accusation flow
- rail patron side play
- persistent save data

Not implemented as actual gameplay systems:

- insurance
- surrender
- dealer soft-17 special handling beyond the simple `hit while value < 17` rule
- a permanent fail state from going broke

One small visual gotcha: the table art includes the text **"INSURANCE PAYS 2 TO 1"**, but there is no insurance action or payout logic in the current game code.

## 16. Practical Strategy Notes

If someone is playing the current implementation rather than just reading the code, the safest high-level advice is:

- use Cheat sparingly because suspicion snowballs fast
- remember that one cheat can already add roughly 18 to 23 suspicion
- once suspicion crosses **35**, cheating can start turning wins into duel risks
- long streaks increase accusation chance further
- Double and Split are powerful, but both commit more bankroll immediately
- the bartender bailout prevents elimination, so bankroll pressure is real but not terminal

## 17. Summary

Blackjack Saloon is not just a blackjack table skin. It is a persistent blackjack loop with:

- standard hand play
- a deterministic hand-improving cheat mechanic
- a persistent suspicion meter
- probabilistic accusations
- reaction-based duel resolution
- rotating opponents
- ambient rail-side simulation

That combination gives the game two layers of strategy:

1. the normal blackjack decision layer
2. the saloon risk-management layer, where cheating can rescue a hand but may provoke the whole room

## 18. How the Code Works

This section explains how the implementation is organized, which classes do what, and how control moves through the program at runtime.

## 19. High-Level Architecture

The code is split into a few clear responsibilities:

- **startup and window setup**
- **UI layout and input handling**
- **game-state orchestration**
- **core blackjack data models**
- **AI behavior for rail patrons**
- **drawing and sound**
- **save/load persistence**
- **tests**

The most important structural idea is that the game logic lives in `GameController`, while the Swing UI asks the controller for a read-only snapshot of current state and renders that snapshot.

## 20. Startup Flow

### `Main`

`Main.java` is the entry point.

- It calls `SwingUtilities.invokeLater(...)`.
- That ensures the Swing UI starts on the Event Dispatch Thread, which is the correct thread for desktop UI work.
- Inside that callback, it creates and shows a `GameFrame`.

### `GameFrame`

`GameFrame.java` owns the main application window.

Its job is to:

- create a `GameController`
- create a `GamePanel`
- install the panel as the frame content
- size and center the window
- wire a global `SPACE` key listener
- trigger the delayed boot transition

The boot transition uses a Swing `Timer` with a 4.2 second delay. During that time the game shows a loading-style overlay. When the timer fires:

- `controller.finishLoading()` is called
- `panel.refresh(true)` redraws the UI with the real starting state

The frame-level `SPACE` key has two meanings:

- advance long narrative text if a story page is waiting
- trigger duel draw input if a duel is active

## 21. UI Layer

### `GamePanel`

`GamePanel.java` is the main Swing UI class. It is large because it combines:

- overall layout
- sidebar and controls
- narrative text behavior
- button actions
- keyboard shortcuts
- timers for duel and rail pacing
- loading overlay behavior
- coordination with sound effects

Conceptually, it acts as the bridge between the player and the controller.

Its main responsibilities are:

1. ask the controller for a `GameSnapshot`
2. push that snapshot into visible UI widgets
3. translate user actions into controller method calls
4. refresh the screen after every meaningful action

Examples of controller calls triggered by the UI:

- `setBetAndDeal(...)`
- `hit()`
- `stand()`
- `doubleDown()`
- `split()`
- `cheat()`
- `drawDuel()`
- `resetSave()`

The panel also contains the in-game tutorial text, so part of the game explanation lives directly in code inside `buildTutorialText()`.

### `TableCanvas`

`TableCanvas.java` is the custom renderer for the saloon table scene.

Instead of using stock Swing widgets for the table, this class paints the room and felt manually with Java2D. It draws:

- the room backdrop
- dealer and rail-seat characters
- the dealer cards
- the player hand area
- rail patron cards and chip stacks
- the shared pot display
- prompts like duel warnings or next-action hints

It does not decide game rules. It only renders the current `GameSnapshot`.

This separation is good design for the project because:

- game rules stay in one place
- the renderer stays visual
- UI refreshes can happen without mutating gameplay state

## 22. Snapshot-Driven Rendering

### `GameSnapshot`

`GameSnapshot.java` is a record that packages everything the UI needs to display.

It includes:

- opponent info
- dealer cards and values
- rail-seat status
- player hands and bets
- bankroll, streak, suspicion, wins, losses
- current status and event text
- booleans for whether actions are allowed
- duel and loading flags

The controller creates a fresh snapshot in `getSnapshot()`.

This pattern is useful because the UI does not reach into controller internals directly. Instead, it receives a shaped view of the state. That makes the rendering code simpler and helps reduce accidental coupling.

## 23. Game Logic Core

### `GameController`

`GameController.java` is the heart of the program.

It owns the runtime state for:

- the `DeckShoe`
- the `Player`
- the `Dealer`
- the current opponent
- the rail patrons
- total wins and losses
- active hand index
- whether a round is active or resolved
- whether a duel is active
- whether rail turns are still being processed
- status and event text
- save persistence access

If you want to understand the game, this is the first file to study.

### What `GameController` does

At a high level, the controller:

1. loads save data during construction
2. chooses a random opponent
3. validates user actions
4. mutates the game state
5. resolves payouts and penalties
6. updates streak and suspicion values
7. persists progress after important state changes
8. exposes a `GameSnapshot` for the UI

### Important controller methods

`finishLoading()`

- Ends the opening boot state.
- Builds the first narrative/event text shown to the player.

`setBetAndDeal(int bet)`

- Validates the wager.
- Resets player and dealer hands.
- Opens a new rail round.
- Deducts the player's bet.
- Deals the opening cards.
- Immediately resolves if blackjack appears on the deal.

`hit()`, `stand()`, `doubleDown()`, `split()`, `cheat()`

- These methods implement the player's direct actions.
- Each method first checks whether that action is currently legal.
- If legal, it mutates the current hand, updates status text, and advances the state machine as needed.

`moveToNextHandOrDealer()`

- This is one of the most important helper methods.
- It advances through split hands.
- Once all player hands are finished, it runs the dealer hit loop and then calls `resolveRound()`.

`resolveRound()`

- Compares each player hand to the dealer.
- Applies payouts and loss handling.
- updates win/loss counters and streak state
- decides whether cheating should trigger a duel
- otherwise begins the rail patron turn phase

`beginRailTurns(...)` and `advanceRailTurn()`

- These methods handle post-round AI resolution for the rail patrons.
- The game processes one rail patron at a time so the UI can narrate each outcome cleanly.

`startDuelSequence()`, `armDuel()`, `drawDuel()`, `resolveDuel(...)`

- These methods implement the accusation / quick-draw system.
- The controller tracks whether the player is allowed to draw yet and resolves early draws as automatic losses.

`finalizeRound(...)`

- Applies post-round suspicion cooling.
- handles bankroll bailout if the player fell below the table minimum
- rotates opponents on hot streaks
- saves progress

### Controller state machine idea

The controller effectively behaves like a simple state machine, even though there is no explicit `enum` for states.

Instead, state is represented by booleans such as:

- `loading`
- `roundActive`
- `roundResolved`
- `railTurnsActive`
- `duelActive`
- `duelCanDraw`

The `canHit()`, `canStand()`, `canDouble()`, `canSplit()`, and `canCheat()` helpers use those flags to decide which actions are currently legal.

## 24. Core Data Model Classes

### `Participant`

`Participant.java` is a shared base class for anyone who can hold hands.

It stores:

- a name
- a list of `Hand` objects

It also provides common behavior for:

- resetting hands
- accessing the primary hand
- inserting additional hands during splits

### `Player`

`Player.java` extends `Participant` and adds the player-specific economy and progression state:

- bankroll
- current bet
- per-hand bet tracking
- streak
- best streak
- suspicion

This is where bankroll math and progression counters live. The controller tells the player object when to:

- place a base bet
- duplicate a bet for a split
- double a hand's wager
- record a win, loss, or push
- cool or add suspicion

### `Dealer`

`Dealer.java` is intentionally tiny.

It inherits hand storage from `Participant` and adds one rule:

- `shouldHit()` returns true while the dealer hand value is below 17

### `Hand`

`Hand.java` contains most of the per-hand rules.

It stores:

- the list of `Card` objects
- whether the hand has stood
- whether it has doubled down

Its methods cover:

- card insertion and removal
- blackjack scoring with ace adjustment
- bust and blackjack checks
- split eligibility
- rendering text for visible or hidden cards
- cheat analysis through `findBestCheatSwap()`

The cheat helper is especially interesting. It tries replacing each card with every other rank, scores the resulting total, and chooses the best candidate using `isBetterCheat(...)`. That is why the Cheat feature feels smart rather than random.

### `Card`, `Rank`, and `Suit`

These classes model the deck itself.

- `Rank` defines symbols and numeric values.
- `Suit` defines the suit options.
- `Card` combines a suit and rank and exposes the blackjack value through `getValue()`.

One subtle UI detail: `Card.toString()` prints face cards as `"10"` instead of `"J"`, `"Q"`, or `"K"`. That keeps the table display visually simple.

### `DeckShoe`

`DeckShoe.java` manages the playable deck pool.

Its responsibilities:

- build one or more full decks
- shuffle them
- deal cards
- reshuffle automatically when low on cards

The test helper `stackCardsForNextDeals(...)` is important for deterministic testing. It lets tests preload specific cards so they can verify exact outcomes like blackjack payout, push handling, or split behavior.

## 25. Opponents and Rail AI

### `OpponentProfile`

`OpponentProfile.java` is a small data class that describes the named opponent across the table.

Each profile contains:

- display name
- intro line
- suspicion sensitivity
- aggression
- duel difficulty

The controller uses these values for:

- narrative flavor
- suspicion gain severity
- accusation probability
- duel reward and timing difficulty

### `RailPatron`

`RailPatron.java` represents one ambient side player.

Each patron stores:

- a stack
- current bet
- current hand
- status text
- a behavior strategy

Its lifecycle methods are:

- `openRound(...)`
- `deal(...)`
- `playRound(...)`
- `settleAgainst(...)`
- `resetSession()`

This class contains the actual rail-side turn execution, but it delegates behavior choices to a strategy object.

### `RailPlayStrategy`, `RailStyle`, and `RailTurnPlan`

This is the small strategy-pattern slice of the project.

- `RailPlayStrategy` defines the behavior contract.
- `RailStyle` implements several concrete personalities as enum constants.
- `RailTurnPlan` packages the decisions for a single patron turn.

This is a nice design choice because the patron logic is split into:

- what the patron chooses to do
- how the patron turn is executed

That makes the AI behavior easier to tune without rewriting the full rail-turn engine.

## 26. Persistence

### `SaveManager`

`SaveManager.java` reads and writes progress through a Java `Properties` file.

On load, it restores:

- bankroll
- best streak
- wins
- losses
- current streak
- suspicion

On save, it writes those same values back out to disk.

It also stores an error message string so the controller can surface load/save failures in the narrative panel instead of crashing the program.

## 27. Sound

### `SoundManager`

`SoundManager.java` handles audio effects and music.

It can:

- play one-shot sound effects
- play short randomized voice snippets
- loop background music

A few implementation details worth noting:

- it caches recent play times to avoid spamming the same sound too quickly
- it keeps small clip pools for reusable voice snippets
- it tries to resolve audio paths flexibly by exact name or normalized partial match
- if sound loading fails, it falls back to a standard system beep

This class is separate from gameplay logic, which keeps controller code cleaner.

## 28. Tests

### `ProjectTestRunner`

The repository's automated tests live in `test/ProjectTestRunner.java`.

Instead of using JUnit, this project uses a custom test runner with plain assertions. The test file covers several important behaviors:

- save manager round-tripping
- blackjack payout
- push behavior
- double-down payout logic
- split-hand creation
- duel resolution

The tests matter because several game systems are stateful and easy to break by accident. Using stacked shoes and a fake clock makes those tests deterministic.

## 29. End-to-End Runtime Example

Here is the normal code path for a typical hand:

1. `Main.main(...)` starts Swing.
2. `GameFrame` creates `GameController` and `GamePanel`.
3. The loading timer expires and calls `controller.finishLoading()`.
4. The player presses **Deal In**.
5. `GamePanel.handleDeal()` calls `controller.setBetAndDeal(...)`.
6. The controller resets hands, deducts the bet, prepares rail patrons, and deals cards.
7. The panel calls `refresh(...)`, which pulls a fresh `GameSnapshot`.
8. The player presses **Hit**, **Stand**, **Double**, **Split**, or **Cheat**.
9. Each action mutates state inside `GameController`.
10. Once the player's hands are done, `moveToNextHandOrDealer()` runs the dealer turn.
11. `resolveRound()` applies payouts and checks for accusation/duel logic.
12. If no duel starts, rail turns are advanced and narrated.
13. `finalizeRound(...)` cools suspicion, applies bailout logic if needed, and saves progress.
14. The UI refreshes again and the table is ready for the next hand.

## 30. Design Strengths and Tradeoffs

A few things the code does well:

- the controller cleanly centralizes rule logic
- the snapshot pattern keeps rendering mostly separate from state mutation
- the rail patron AI uses a simple but clear strategy pattern
- save/load behavior is isolated
- tests cover the most fragile rule paths

A few tradeoffs in the current structure:

- `GamePanel` is doing a lot at once and is the biggest candidate for future refactoring
- controller state is managed with several booleans instead of a single explicit game-state enum
- some presentation text lives directly in UI code rather than in separate resources
- the custom test runner is lightweight, but a standard test framework would scale better if the project grows

## 31. If You Are Reading the Code for Class

If this document is being used to explain the project to someone else, the best reading order is:

1. `Main.java`
2. `GameFrame.java`
3. `GameController.java`
4. `Hand.java`
5. `Player.java`
6. `DeckShoe.java`
7. `RailPatron.java`
8. `RailStyle.java`
9. `GameSnapshot.java`
10. `GamePanel.java`
11. `TableCanvas.java`
12. `SaveManager.java`
13. `test/ProjectTestRunner.java`

That order gives the cleanest path from program startup, to rules, to data model, to UI, to verification.
