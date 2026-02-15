#!/bin/bash
set -e

echo "📦 Installing Connect IQ SDK Manager CLI..."
curl -s https://raw.githubusercontent.com/lindell/connect-iq-sdk-manager-cli/master/install.sh \
  | sed 's/sudo //' \
  > /tmp/install.sh

bash /tmp/install.sh
rm /tmp/install.sh

echo "✅ CLI installed. Now run sdk-init.sh to complete setup."
