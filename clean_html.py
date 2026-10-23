import re

def strip_c_style(code):
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

filepath = r"e:\MADPROJECT\web_portal\index.html"
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

def script_replacer(match):
    return "<script" + match.group(1) + ">" + strip_c_style(match.group(2)) + "</script>"

content = re.sub(r'<script(.*?)>(.*?)</script>', script_replacer, content, flags=re.DOTALL | re.IGNORECASE)

def style_replacer(match):
    return "<style" + match.group(1) + ">" + strip_c_style(match.group(2)) + "</style>"

content = re.sub(r'<style(.*?)>(.*?)</style>', style_replacer, content, flags=re.DOTALL | re.IGNORECASE)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Cleaned script and style blocks in index.html")
