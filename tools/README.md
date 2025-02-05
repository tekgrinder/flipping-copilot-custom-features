# Flipping Copilot Tools

This directory contains utility tools for the Flipping Copilot plugin.

## Export/Import Item Lists

The `export_items.py` script allows you to export and import blacklist/whitelist items to/from a CSV file for external editing.

### Prerequisites
- Python 3.6 or higher
- RuneLite with Flipping Copilot plugin installed and configured

### Usage

1. **Export current items:**
   ```bash
   python export_items.py
   ```
   This will create a `tradeable_items.csv` file with your current blacklist/whitelist items.

2. **Edit the CSV file:**
   - Open `tradeable_items.csv` in your preferred spreadsheet editor
   - Each row contains:
     - `item_id`: The RuneScape item ID
     - `name`: Item name (may show as "Unknown" in export)
     - `is_filtered`: Set to "True" to include in blacklist/whitelist, "False" to exclude

3. **Import modified items:**
   ```bash
   python export_items.py --import
   ```
   This will update your plugin preferences with the modified item list.

### Notes
- The script automatically detects whether you're using blacklist or whitelist mode
- Changes are saved to your RuneLite preferences file
- Make sure to restart the plugin after importing changes
- Back up your preferences file before making changes