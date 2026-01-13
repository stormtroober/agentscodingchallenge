# Installation Guide

## System Installation

### Linux Installation
To install the application on Linux (Ubuntu/Debian/CentOS), follow these steps:

1. **Download the package**:
   ```bash
   wget https://download.example.com/linux/app-latest.tar.gz
   ```

2. **Extract the archive**:
   ```bash
   tar -xzf app-latest.tar.gz
   cd app-latest
   ```

3. **Run the installer script**:
   ```bash
   sudo ./install.sh
   ```

4. **Start the service**:
   ```bash
   sudo systemctl start app-service
   ```

### Windows Installation
1. Download the `.msi` installer from our website.
2. Double-click to run the setup wizard.
3. Follow the on-screen instructions.

### macOS Installation
1. Download the `.dmg` file.
2. Drag the application icon to your Applications folder.

## SDK Installation

### Java
Add the dependency to your `pom.xml`:
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Python
```bash
pip install example-sdk
```

### Node.js
```bash
npm install @example/sdk
```
