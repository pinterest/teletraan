# Copyright 2016 Pinterest, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import sys
import subprocess
import os
import glob

parent_path_generator = lambda _path, x: os.sep.join(_path.split(os.sep)[:-x])
api_path = '%s/%s' % (parent_path_generator(os.path.dirname(os.path.abspath(__file__)), 1), 'api')
beans_path = '%s/%s' % (parent_path_generator(os.path.dirname(os.path.abspath(__file__)), 2),
                        'deploy-service/common/src/main/java/com/pinterest/deployservice/bean')

def generate_big_docs():
    """
    Uses Swagger2Markup to generate definitions.md, overview.md, and paths.md
    """
    if len(sys.argv) > 1:
        swagger_json_input = sys.argv[1]
        generate_cmd = ['java', '-jar', 'build/libs/swagger2markup-cli-1.3.3.jar', 'convert', '-i', swagger_json_input,
                        '-d', api_path, '-c', 'config.properties']
        process = subprocess.Popen(generate_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        output, error = process.communicate()
        print(output)
        if error and 'SLF4J' not in error:
            print("Error generating Swagger2Markup docs:", error)


def remove_old_docs():
    """
    Removes old docs in the API folder
    """
    files = [f for f in os.listdir(api_path) if f.endswith('.md')]
    for f in files:
        os.remove('%s/%s' % (api_path, f))


def write_new_docs():
    """
    Splits up the paths.md file into multiple files, by resource tag
    """
    with open(api_path + '/paths.md') as f:
        lines = f.readlines()
    new_file = None
    for line in lines:
        if line.startswith('### '):
            new_file = open("%s/%s%s" % (api_path, line[3:].strip().replace(' ', ''), '.md'), "a")
        if new_file:
            new_file.write(line)
    os.remove('%s/%s' % (api_path, 'paths.md'))


def write_enum_docs():
    """
    Because Swagger2Markup doesn't include enum as APIModelDefinitions, we must parse the enum files and create
    definitions for them
    """
    definitions_file = open('%s/%s' % (api_path, 'definitions.md'), 'a')
    definitions_file.write('\n## Enums\n')
    for file_path in glob.glob(os.path.join(beans_path, '*.*')):
        filename = file_path.split('/')[-1].replace('.java', '')
        if 'bean' not in filename.lower() and 'public enum' in open(file_path).read():
            definitions_file.write('\n### ' + filename + '\n')
            with open(file_path) as f:
                lines = f.readlines()
                for line in lines:
                    if 'public enum' in line or '*/' in line:
                        break
                    elif '*' in line and '/**' not in line:
                        definitions_file.write('\n' + line.replace('*', ''))

if __name__ == "__main__":
    remove_old_docs()
    generate_big_docs()
    write_new_docs()
    write_enum_docs()
    print("Finished generating API docs!")