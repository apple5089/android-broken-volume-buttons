"""Build a native Android APK offline with JDK 17 and Android SDK 34."""
import argparse
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import zipfile

HERE = Path(__file__).resolve().parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--sdk', default=os.getenv('ANDROID_HOME') or os.getenv('ANDROID_SDK_ROOT'))
parser.add_argument('--jdk', default=os.getenv('JAVA_HOME'))
parser.add_argument('--project', type=Path, default=HERE)
parser.add_argument('--name', default='easy-volume-1.1.apk')
parser.add_argument('--keystore', type=Path, help='Existing private release keystore; never commit it')
parser.add_argument('--alias', default='easyvolume')
args = parser.parse_args()
if not args.sdk:
    parser.error('Provide --sdk or set ANDROID_HOME / ANDROID_SDK_ROOT')
if not args.jdk:
    javac = shutil.which('javac')
    if not javac:
        parser.error('Provide --jdk or set JAVA_HOME to JDK 17')
    args.jdk = str(Path(javac).resolve().parents[1])
if Path(args.name).name != args.name or not args.name.endswith('.apk'):
    parser.error('--name must be an APK filename without directory components')
root, sdk, jdk = args.project.resolve(), Path(args.sdk).resolve(), Path(args.jdk).resolve()
suffix = '.exe' if os.name == 'nt' else ''
bt = sdk / 'build-tools/34.0.0'
android = sdk / 'platforms/android-34/android.jar'

def java(tool):
    return jdk / 'bin' / (tool + suffix)

for required in [android, bt/('aapt2'+suffix), bt/('zipalign'+suffix), bt/'lib/d8.jar',
                 bt/'lib/apksigner.jar', bt/'core-lambda-stubs.jar', java('javac'), java('java'), java('keytool')]:
    if not required.is_file():
        parser.error(f'Missing tool: {required}')
work, out = root/'build', root/'dist'
work.mkdir(parents=True, exist_ok=True)
out.mkdir(parents=True, exist_ok=True)
# Separate compilation directories prevent stale bytecode from earlier source revisions.
compile_dir = Path(tempfile.mkdtemp(prefix='compile-', dir=work))
classes, dex_dir = compile_dir/'classes', compile_dir/'dex'
classes.mkdir(); dex_dir.mkdir()
env = dict(os.environ, JAVA_HOME=str(jdk))
env['PATH'] = str(jdk/'bin') + os.pathsep + env.get('PATH', '')

def run(*cmd):
    result = subprocess.run([str(x) for x in cmd], cwd=root, env=env,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            encoding='utf-8', errors='replace')
    print(result.stdout, end='')
    if result.returncode:
        raise SystemExit(result.returncode)
    return result.stdout

run(bt/('aapt2'+suffix), 'compile', '--dir', root/'res', '-o', compile_dir/'res.zip')
run(java('javac'), '-J-Dfile.encoding=UTF-8', '-encoding', 'UTF-8', '-source', '8', '-target', '8',
    '-bootclasspath', str(android)+os.pathsep+str(bt/'core-lambda-stubs.jar'),
    '-d', classes, *sorted((root/'src').rglob('*.java')))
run(java('java'), '-cp', bt/'lib/d8.jar', 'com.android.tools.r8.D8', '--lib', android,
    '--min-api', '26', '--output', dex_dir, *sorted(classes.rglob('*.class')))
run(bt/('aapt2'+suffix), 'link', '--manifest', root/'AndroidManifest.xml', '-I', android,
    '-o', compile_dir/'unsigned.apk', compile_dir/'res.zip')
with zipfile.ZipFile(compile_dir/'unsigned.apk', 'a', zipfile.ZIP_DEFLATED) as z:
    for dex in dex_dir.glob('*.dex'):
        z.write(dex, dex.name)
run(bt/('zipalign'+suffix), '-f', '4', compile_dir/'unsigned.apk', compile_dir/'aligned.apk')

key = args.keystore.resolve() if args.keystore else work/'local-signing.keystore'
if args.keystore and not key.is_file():
    parser.error('--keystore must point to an existing private keystore')
if args.keystore and not env.get('EASY_VOLUME_KEYSTORE_PASSWORD'):
    parser.error('Set EASY_VOLUME_KEYSTORE_PASSWORD for the supplied release keystore')
env.setdefault('EASY_VOLUME_KEYSTORE_PASSWORD', 'local-development-only')
if not key.exists():
    run(java('keytool'), '-genkeypair', '-keystore', key,
        '-storepass:env', 'EASY_VOLUME_KEYSTORE_PASSWORD', '-keypass:env', 'EASY_VOLUME_KEYSTORE_PASSWORD',
        '-alias', args.alias, '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000',
        '-dname', 'CN=Broken Volume Buttons Local Development')
apk = out/args.name
run(java('java'), '-jar', bt/'lib/apksigner.jar', 'sign', '--ks', key,
    '--ks-key-alias', args.alias, '--ks-pass', 'env:EASY_VOLUME_KEYSTORE_PASSWORD',
    '--key-pass', 'env:EASY_VOLUME_KEYSTORE_PASSWORD', '--out', apk, compile_dir/'aligned.apk')
evidence = run(java('java'), '-jar', bt/'lib/apksigner.jar', 'verify', '--verbose', '--print-certs', apk)
evidence += run(bt/('zipalign'+suffix), '-c', '4', apk)
(out/'signature-verification.txt').write_text(evidence, encoding='utf-8')
(out/'manifest-badging.txt').write_text(run(bt/('aapt2'+suffix), 'dump', 'badging', apk), encoding='utf-8')
(out/'SHA256.txt').write_text(hashlib.sha256(apk.read_bytes()).hexdigest()+'  '+apk.name+'\n', encoding='utf-8')
print(f'APK: {apk}')
