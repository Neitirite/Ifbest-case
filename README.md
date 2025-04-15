# IfBest-Case
## Установка
### 1.Установка MiniO
1. Разверните в Docker хранилище MiniO:
```
docker run \
   -p 9000:9000 \
   -p 9001:9001 \
   --name minio \
   -v ~/minio/data:/data \
   -e "MINIO_ROOT_USER=YourRootName" \
   -e "MINIO_ROOT_PASSWORD=YourRootPassword" \
   quay.io/minio/minio server /data --console-address ":9001"
```
2. Перейдите по ссылке `http://ip:9000` и войдите в аккаунт. Перейдите в пункт `Access keys` и создайте новый ключ. Сохраните Access Key и Secret Key, они потребуются в будущем

### 2. Установка бэкенда
