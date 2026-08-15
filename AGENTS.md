# Incident Management System

## Environment notes

### Running tests

Use `./scripts/test.sh` (sets up the embedded MongoDB environment) instead of
`./mvnw test` on machines that lack OpenSSL 1.1.

On distributions with OpenSSL 3.x only (e.g. Arch/CachyOS), the mongod binary
that de.flapdoodle downloads is the Ubuntu 20.04 build, which links against
`libssl.so.1.1`/`libcrypto.so.1.1`. `scripts/test.sh` exports `LD_LIBRARY_PATH`
pointing at `~/.local/share/openssl-1.1` when that directory exists.

To provision those libraries on a new machine (rootless, from the Ubuntu focal
package):

```sh
curl -sL -o /tmp/libssl11.deb https://security.ubuntu.com/ubuntu/pool/main/o/openssl/libssl1.1_1.1.1f-1ubuntu2.24_amd64.deb
mkdir -p ~/.local/share/openssl-1.1 /tmp/ssl11
bsdtar -xf /tmp/libssl11.deb -C /tmp/ssl11
bsdtar -xf /tmp/ssl11/data.tar.xz -C /tmp/ssl11
cp /tmp/ssl11/usr/lib/x86_64-linux-gnu/lib{crypto,ssl}.so.1.1 ~/.local/share/openssl-1.1/
```

The embedded Mongo version is pinned via `de.flapdoodle.mongodb.embedded.version`
in `notification-service` test resources. It must be a version the flapdoodle
packageresolver ships for this platform (see the test yaml).
