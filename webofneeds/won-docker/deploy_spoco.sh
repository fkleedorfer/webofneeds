# fail the whole script if one command fails
set -e

# base folder is used to mount some files (e.g. certificates) from the server into the containers
export base_folder=/usr/share/webofneeds/spoco
export live_base_folder=/home/won/matchat

# check if all application data should be removed before deployment
if [ "$remove_all_data" = true ] ; then

  echo generating new certificates! Old files will be deleted!
  ssh root@satvm02 rm -rf $base_folder/won-server-certsspoco
  ssh root@satvm02 rm -rf $base_folder/won-client-certs

  echo delete postgres, bigdata and solr databases!
  ssh root@satvm02 rm -rf $base_folder/postgres/data
fi

ssh root@satvm02 mkdir -p $base_folder/won-server-certsspoco
ssh root@satvm02 mkdir -p $base_folder/won-client-certs

# create a password file for the certificates, variable ${won_certificate_passwd} must be set from outside the script
# note: name of the password file is fixed in won-docker/image/nginx/nginx.conf
echo ${won_certificate_passwd} > won_certificate_passwd_file
ssh root@satvm02 mkdir -p $base_folder/won-server-certsspoco
scp won_certificate_passwd_file root@satvm02:$base_folder/won-server-certsspoco/won_certificate_passwd_file
rm won_certificate_passwd_file

# copy the nginx.conf file to the proxy server
scp $WORKSPACE/webofneeds/won-docker/image/nginx/nginx.conf won@satvm01:$live_base_folder/nginx.conf


# copy the spoco skin to the custom skin folder that get used by this instance
ssh root@satvm02 mkdir -p $base_folder/custom_owner_skin
scp -r $WORKSPACE/webofneeds/won-owner-webapp/src/main/webapp/skin/spoco/* root@satvm02:$base_folder/custom_owner_skin/

# copy the openssl.conf file to the server where the certificates are generated
# the conf file is needed to specify alternative server names, see conf file in won-docker/image/gencert/openssl.conf
scp $WORKSPACE/webofneeds/won-docker/image/gencert/openssl.conf root@satvm02:$base_folder/openssl.conf

# copy letsencrypt certificate files from satvm01 (live/matchat) to satvm02
ssh root@satvm02 mkdir -p $base_folder/letsencrypt/certs/live/matchat.org
scp -3 won@satvm01:$live_base_folder/letsencrypt/certs/live/matchat.org/* root@satvm02:$base_folder/letsencrypt/certs/live/matchat.org/

# TODO: change the explicit passing of tls params when docker-compose bug is fixed: https://github.com/docker/compose/issues/1427
echo run docker containers using docker-compose on satvm02
#docker --tlsverify -H satvm02.researchstudio.at:2376 pull webofneeds/bigdata
cd deploy/spoco_satvm02
docker-compose --tlsverify --tlscacert=/var/lib/jenkins/.docker/ca.pem --tlscert=/var/lib/jenkins/.docker/cert.pem --tlskey=/var/lib/jenkins/.docker/key.pem -H satvm02.researchstudio.at:2376 down
docker-compose --tlsverify --tlscacert=/var/lib/jenkins/.docker/ca.pem --tlscert=/var/lib/jenkins/.docker/cert.pem --tlskey=/var/lib/jenkins/.docker/key.pem -H satvm02.researchstudio.at:2376 up --build -d

