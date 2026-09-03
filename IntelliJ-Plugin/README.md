# Student-testing-IntelliJ-plugin

![Build](https://github.com/RAFSoftLab/Student-testing-IntelliJ-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

<!-- Plugin description -->
RAF LMS companion plugin for programming exams. Students fetch their exam assignment
straight into IntelliJ IDEA, work on it in a prepared project, and submit it (intermediate
commit or final submission) from a dedicated tool window. The plugin also records activity
during the exam and sends aggregated metrics to the instructor dashboard.

The working session is persisted between IDE runs, so after a restart a student keeps
working and can submit without starting the assignment over.
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Student-testing-IntelliJ-plugin"</kbd> >
  <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/RAFSoftLab/Student-testing-IntelliJ-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
