#!/usr/bin/env python3

import json
import os
import re
import shutil
import sys

base_dir = os.path.dirname(sys.path[0])
os.chdir(base_dir)

db_path = 'data/irext_db_20260215_mysql.sql'
out_dir = 'app/src/main/assets/tv_brands_remotes'

def parse_mysql_insert(line):
    values = []
    in_str = False
    escape = False
    curr_tuple = []
    curr_val = []

    idx = line.find('VALUES')
    if idx == -1: return []
    i = line.find('(', idx)

    in_tuple = False
    while i < len(line):
        c = line[i]
        if not in_str:
            if c == '(':
                if not in_tuple:
                    in_tuple = True
                    curr_tuple = []
                    curr_val = []
                else: curr_val.append(c)
            elif c == ')':
                if in_tuple:
                    curr_tuple.append("".join(curr_val))
                    values.append(curr_tuple)
                    in_tuple = False
                else: curr_val.append(c)
            elif c == "'":
                in_str = True
            elif c == ',':
                if in_tuple:
                    curr_tuple.append("".join(curr_val))
                    curr_val = []
            else:
                if in_tuple: curr_val.append(c)
        else:
            if escape:
                curr_val.append(c)
                escape = False
            elif c == '\\':
                escape = True
            elif c == "'":
                if i + 1 < len(line) and line[i+1] == "'":
                    curr_val.append("'")
                    i += 1
                else:
                    in_str = False
            else:
                curr_val.append(c)
        i += 1
    return [[v.strip() for v in t] for t in values]

if not os.path.exists(db_path):
    print(f"File {db_path} not found!")
    exit(1)

brands = {}
remotes = {}
remote_keys = {}

print("Parsing SQL dump... this may take a minute.")
with open(db_path, 'r', encoding='utf-8') as f:
    for line in f:
        if 'INSERT INTO `brand`' in line or 'INSERT INTO brand' in line:
            for row in parse_mysql_insert(line):
                if len(row) > 7 and row[2] == '2':
                    name = row[7] if row[7] and row[7] != 'NULL' else row[1]
                    brands[row[0]] = name

        elif 'INSERT INTO `remote_index`' in line or 'INSERT INTO remote_index' in line:
            for row in parse_mysql_insert(line):
                if len(row) > 15 and row[1] == '2':
                    brand_id = row[3]
                    if brand_id not in remotes:
                        remotes[brand_id] = []
                    remotes[brand_id].append({
                        'id': int(row[0]),
                        'remote_number': row[15],
                        'remote_map': row[11],
                        'remote': row[10],
                        'protocol': row[9]
                    })

        elif 'INSERT INTO `decode_remote`' in line or 'INSERT INTO decode_remote' in line:
            for row in parse_mysql_insert(line):
                if len(row) > 8 and row[1] == '2':
                    remote_id = int(row[5])
                    key_name = row[7].lower()
                    key_val_str = row[8]
                    if key_val_str and key_val_str != 'NULL':
                        try:
                            timings = [int(x.strip()) for x in key_val_str.split(',') if x.strip()]
                            if remote_id not in remote_keys:
                                remote_keys[remote_id] = {}
                            if key_name not in remote_keys[remote_id]:
                                remote_keys[remote_id][key_name] = []
                            remote_keys[remote_id][key_name].append(timings)
                        except ValueError:
                            pass

if os.path.exists(out_dir):
    shutil.rmtree(out_dir)
os.makedirs(out_dir, exist_ok=True)

print(f"Generating JSON files in {out_dir}...")
for brand_id, brand_name in brands.items():
    brand_remotes = remotes.get(brand_id, [])
    if not brand_remotes:
        continue

    valid_remotes = []
    for r in brand_remotes:
        r['keys'] = remote_keys.get(r['id'], {})
        if r['keys']:
            valid_remotes.append(r)

    if not valid_remotes:
        continue

    safe_name = "".join(c for c in brand_name if c.isalnum() or c in (' ', '-', '_')).strip()
    filename = f"{brand_id}_{safe_name}.json"

    data = {
        'id': int(brand_id),
        'name_en': brand_name,
        'remotes': valid_remotes
    }

    with open(os.path.join(out_dir, filename), 'w', encoding='utf-8') as f:
        # save pretty-printed JSON
        dump_str = json.dumps(data, indent=2)
        # replace array of integers on multiple lines by one line using regex
        compact_str = re.sub(
            r'\[\s+([\d\s,-]+?)\s+\]',
            lambda m: '[' + ', '.join(x.strip() for x in m.group(1).split(',') if x.strip()) + ']',
            dump_str
        )
        f.write(compact_str)

print("Done!")
