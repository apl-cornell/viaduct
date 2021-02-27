# Logging

We use the [kotlin-logging](https://github.com/MicroUtils/kotlin-logging)
library for showing additional information to the user. Logs go to standard error in accordance with Unix conventions,
and the user can control the granularity of logs using the `--verbose` flag.

As a general rule, *never* use `print()` or `println()` to display information to the user. This includes showing
information to yourself for debugging. All logging frameworks have a `DEGUB` level, and if you found this information
useful, chances are it will be relevant later.

Logging is extremely easy to use. See [this section](https://github.com/MicroUtils/kotlin-logging#getting-started).
