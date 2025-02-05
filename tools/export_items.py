import json
import csv
import os
import glob

def find_preferences_file():
    """Find the most recently modified preferences file."""
    parent_dir = os.path.expanduser("~/.runelite/flipping-copilot")
    if not os.path.exists(parent_dir):
        raise FileNotFoundError(f"RuneLite directory not found: {parent_dir}")
    
    pref_files = glob.glob(os.path.join(parent_dir, "acc_*_preferences.json"))
    if not pref_files:
        raise FileNotFoundError(f"No preference files found in {parent_dir}")
    
    return max(pref_files, key=os.path.getmtime)

def load_preferences():
    """Load the preferences file."""
    pref_file = find_preferences_file()
    with open(pref_file, 'r') as f:
        return json.load(f)

def save_preferences(prefs, output_file):
    """Save the preferences back to the file."""
    pref_file = find_preferences_file()
    with open(pref_file, 'w') as f:
        json.dump(prefs, f, indent=2)
    print(f"Updated preferences saved to: {pref_file}")

def export_to_csv(prefs, output_file):
    """Export the preferences to a CSV file."""
    # Headers for the CSV
    headers = ['item_id', 'name', 'is_filtered']
    
    # Get the filtered items list based on mode
    is_whitelist = prefs.get('whitelistMode', False)
    filtered_items = set(prefs.get('whitelistedItemIds', []) if is_whitelist else prefs.get('blockedItemIds', []))
    
    # Write to CSV
    with open(output_file, 'w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=headers)
        writer.writeheader()
        
        # Write the data
        for item_id in filtered_items:
            writer.writerow({
                'item_id': item_id,
                'name': 'Unknown',  # We don't have item names without game client
                'is_filtered': True
            })

def import_from_csv(input_file):
    """Import preferences from a CSV file."""
    items = []
    with open(input_file, 'r', newline='') as f:
        reader = csv.DictReader(f)
        for row in reader:
            if row['is_filtered'].lower() == 'true':
                items.append(int(row['item_id']))
    return items

def main():
    # Load current preferences
    try:
        prefs = load_preferences()
    except FileNotFoundError as e:
        print(f"Error: {e}")
        return

    # Export to CSV
    output_file = "tradeable_items.csv"
    export_to_csv(prefs, output_file)
    print(f"Exported to: {output_file}")
    
    # Instructions for the user
    print("\nInstructions:")
    print("1. Edit the CSV file 'tradeable_items.csv'")
    print("2. Set 'is_filtered' to 'True' or 'False' for each item")
    print("3. Run this script with --import flag to update preferences")
    print("\nUsage:")
    print("Export: python export_items.py")
    print("Import: python export_items.py --import")

if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1 and sys.argv[1] == "--import":
        try:
            prefs = load_preferences()
            input_file = "tradeable_items.csv"
            filtered_items = import_from_csv(input_file)
            
            # Update the appropriate list based on mode
            if prefs.get('whitelistMode', False):
                prefs['whitelistedItemIds'] = filtered_items
            else:
                prefs['blockedItemIds'] = filtered_items
                
            save_preferences(prefs, input_file)
            print("Successfully imported items from CSV")
        except Exception as e:
            print(f"Error importing: {e}")
    else:
        main()