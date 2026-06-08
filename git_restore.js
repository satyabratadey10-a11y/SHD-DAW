const { execSync } = require('child_process');
try {
  execSync('git checkout .', { stdio: 'inherit' });
  execSync('git clean -fd', { stdio: 'inherit' });
} catch (e) {
  console.error(e);
}
