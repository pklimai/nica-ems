if [[ $(sudo docker ps -a -q --filter "name=nica-emd") ]]; then
sudo docker stop $(sudo docker ps -a -q --filter "name=nica-emd") &>/dev/null; sudo docker rm $(sudo docker ps -a -q --filter "name=nica-emd") &>/dev/null
echo '--- Containers have been cleared'
fi
sh gradlew installDist
sudo docker-compose up --build -d

