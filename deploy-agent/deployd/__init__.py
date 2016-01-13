import os

# Pinterest specific settings
IS_PINTEREST = True if os.getenv("IS_PINTEREST", "false") == "true" else False
