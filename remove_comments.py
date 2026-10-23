import os
import re

def strip_c_style(code):
    # Matches strings or comments
    pattern = re.compile(
        r'//.*?$|/\*.*?\*/|\'(?:\\.|[^\\\'])*\'|"(?:\\.|[^\\"])*"',
        re.DOTALL | re.MULTILINE
    )
    def replacer(match):
        s = match.group(0)
        if s.startswith('/'):
            return ""
        return s
    return re.sub(pattern, replacer, code)

def strip_html_xml(code):
    # Removes <!-- -->
    pattern = re.compile(r'<!--.*?-->', re.DOTALL)
    # Note: this won't remove JS comments inside <script> blocks in HTML.
    return re.sub(pattern, '', code)

def strip_python(code):
    pattern = re.compile(
        r'#.*?$|"""(?:\\.|[^"\\])*"""|\'\'\'(?:\\.|[^\'\\])*\'\'\'|\'(?:\\.|[^\\\'])*\'|"(?:\\.|[^\\"])*"',
        re.DOTALL | re.MULTILINE
    )
    def replacer(match):
        s = match.group(0)
        if s.startswith('#'):
            return ""
        return s
    return re.sub(pattern, replacer, code)

def process_file(filepath):
    ext = os.path.splitext(filepath)[1].lower()
    
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            code = f.read()
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return

    new_code = code
    if ext in ['.java', '.js', '.css']:
        new_code = strip_c_style(code)
    elif ext in ['.html', '.xml']:
        new_code = strip_html_xml(code)
    elif ext in ['.py']:
        new_code = strip_python(code)
    else:
        return # Skip other files

    if new_code != code:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_code)
        print(f"Removed comments from {filepath}")

def main():
    base_dir = r"e:\MADPROJECT"
    targets = [
        os.path.join(base_dir, "app", "src"),
        os.path.join(base_dir, "web_portal"),
        os.path.join(base_dir, "backend")
    ]
    
    for target in targets:
        if not os.path.exists(target):
            continue
        for root, dirs, files in os.walk(target):
            if 'venv' in dirs:
                dirs.remove('venv') # Don't traverse venv
            if '__pycache__' in dirs:
                dirs.remove('__pycache__')
            for f in files:
                filepath = os.path.join(root, f)
                process_file(filepath)

if __name__ == "__main__":
    main()
