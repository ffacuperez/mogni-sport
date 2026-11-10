#!/usr/bin/env bash
# Copia de la base de Mogni (formato de pg_dump, comprimida) en ~/backups, con fecha y hora en el nombre.
# Guarda los últimos 14 días. En el servidor corre todos los días a las 03:00 (crontab del usuario):
#   0 3 * * * $HOME/mogni-sport/scripts/backup-base.sh >> $HOME/backups/backup.log 2>&1
#
# Restaurar una copia (BORRA lo que haya en la base y deja lo de la copia):
#   cd ~/mogni-sport && docker compose stop backend
#   docker compose exec -T db sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner' < ~/backups/ARCHIVO.dump
#   docker compose start backend
set -euo pipefail

DIR="$HOME/backups"
DIAS=14
umask 077
mkdir -p "$DIR"

ARCHIVO="$DIR/mogni-$(date +%F-%H%M).dump"
cd "$(dirname "$0")/.."
/usr/bin/docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' < /dev/null > "$ARCHIVO.tmp"
# Recién completa toma el nombre final: una copia cortada a la mitad nunca parece buena
mv "$ARCHIVO.tmp" "$ARCHIVO"
find "$DIR" -name 'mogni-*.dump' -mtime +"$DIAS" -delete

echo "$(date '+%F %T') copia ok: $(basename "$ARCHIVO") ($(du -h "$ARCHIVO" | cut -f1))"
