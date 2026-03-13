## Core Classes

**StudentEvent.java**
- Data model that represents a single tracked event (like "student clicked", "code changed", etc.)
- Contains timestamp, student ID, event type, and additional data

**EventTracker.java**
- Main coordinator that starts/stops tracking and manages all listeners
- Provides `logEvent()` method that other classes use to record events
- Creates and initializes all the specific tracker classes

**EventQueueManager.java**
- Handles the local queue of events on each student's PC
- Runs background threads to process events and send them in batches
- Ensures events aren't lost if network fails temporarily

**EventTransmissionService.java**
- Handles sending event batches to your backend server via HTTP
- Implements retry logic if network requests fail
- Saves events to local backup files if server is unreachable

## Listener Classes (in `/listeners/` folder)

**FocusEventTracker.java**
- Tracks when student switches away from IntelliJ or back to it
- Helps measure attention and focus time

**CodeChangeTracker.java**
- Tracks every time student types, deletes, or modifies code
- Records which file was changed and how much

**CompilationTracker.java**
- Tracks when student tries to build/compile their code
- Records success/failure, number of errors and warnings

**TestExecutionTracker.java**
- Tracks when student runs tests or executes their program
- Records whether execution succeeded or failed

**ActivityTracker.java**
- Tracks general mouse clicks and keyboard activity
- Helps measure overall engagement (but doesn't record actual keystrokes for privacy)

**Think of it like this analogy**: If you were observing students in a physical classroom,
these classes would be like different "observers" - one watching when they look away
from their paper, another noting when they erase something,
another watching when they test their solution, etc.
All reporting back to a central "supervisor" (EventTracker) who coordinates everything.