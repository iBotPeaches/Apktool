# Apktool in Docker
We provide an easy way to leverage `apktool`, along with common Android tools such as `zipalign` and `apksigner`, all from within Docker.

## Building the Docker image
To use the image, pull or build with the included Dockerfile:
```bash
docker pull ghcr.io/ibotpeaches/apktool:latest
# OR
docker build -t apktool:local .
```

## Using the Docker image
The best way to use the image is to create aliases to run the internal commands. Replace `ghcr.io/ibotpeaches/apktool:latest` with `apktool:local` if you have built the image locally.
```bash
alias apktool="docker run --rm -ti --name=apktool -v \"${PWD}:${PWD}\" -w \"${PWD}\" ghcr.io/ibotpeaches/apktool:latest apktool"
alias zipalign="docker run --rm -ti --name=zipalign -v \"${PWD}:${PWD}\" -w \"${PWD}\" ghcr.io/ibotpeaches/apktool:latest zipalign"
alias apksigner="docker run --rm -ti --name=apksigner -v \"${PWD}:${PWD}\" -w \"${PWD}\" ghcr.io/ibotpeaches/apktool:latest apksigner"
```

## Running the commands
You can then utilize these commands as you would if they were natively installed:
```bash
apktool d My.apk -o MyFolder
apktool b MyFolder -o MyNew.apk
zipalign -p -f 4 MyNew.apk MyNewAligned.apk
apksigner sign --ks My.keystore MyNewAligned.apk
```
