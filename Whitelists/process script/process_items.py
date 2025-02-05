#!/usr/bin/env python3
import os
import csv
import re

def list_items(directory):
    """List all files in the items directory."""
    return [f for f in os.listdir(directory) if os.path.isfile(os.path.join(directory, f))]

def clean_filename(filename):
    """Clean filename by removing underscores, extensions, and numbers (except those in brackets)."""
    # Remove file extension
    name = os.path.splitext(filename)[0]
    
    # First replace underscores with spaces
    name = name.replace('_', ' ')
    
    # Remove numbers that are not in brackets
    # This regex matches numbers that are not inside parentheses
    name = re.sub(r'(?<!\()\b\d+\b(?!\))', '', name)
    
    # Clean up any double spaces and trim
    name = ' '.join(name.split())
    
    return name

def read_complete_list(file_path):
    """Read the complete list CSV file and create a mapping of names to IDs."""
    item_ids = {}
    with open(file_path, 'r') as f:
        csv_reader = csv.reader(f)
        next(csv_reader)  # Skip the mode line
        next(csv_reader)  # Skip the header
        for row in csv_reader:
            if len(row) >= 2:
                item_ids[row[1]] = row[0]
    return item_ids

def process_items(items_dir, complete_list_path):
    """Process all items and generate output files."""
    # Get list of files
    files = list_items(items_dir)
    
    # Clean filenames
    cleaned_names = [clean_filename(f) for f in files]
    
    # Read complete list
    item_ids = read_complete_list(complete_list_path)
    
    # Match items and prepare output
    matched = []
    unmatched = []
    
    for name in cleaned_names:
        if name in item_ids:
            matched.append((item_ids[name], name))
        else:
            unmatched.append(name)
    
    # Write matched items to CSV
    with open('matched_items.csv', 'w', newline='') as f:
        f.write("# Mode: whitelist\n")
        f.write("item_id,name,is_filtered\n")
        for item_id, item_name in matched:
            f.write(f"{item_id},{item_name},true\n")
    
    # Write unmatched items to text file
    with open('unmatched_items.txt', 'w') as f:
        for item in unmatched:
            f.write(f"{item}\n")
    
    # Print summary
    print(f"\nSummary:")
    print(f"Successfully matched: {len(matched)} items")
    print(f"Failed to match: {len(unmatched)} items")
    print(f"\nMatched items have been saved to 'matched_items.csv'")
    print(f"Unmatched items have been saved to 'unmatched_items.txt'")

def main():
    items_dir = os.path.join(os.getcwd(), 'items')
    complete_list = 'complete list.csv'
    
    if not os.path.exists(items_dir):
        print(f"Error: Items directory '{items_dir}' not found")
        return
    
    if not os.path.exists(complete_list):
        print(f"Error: Complete list file '{complete_list}' not found")
        return
    
    process_items(items_dir, complete_list)

if __name__ == '__main__':
    main()