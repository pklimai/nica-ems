if [[ $(sudo docker ps -a -q --filter "name=nica-ems") ]]; then
sudo docker stop $(sudo docker ps -a -q --filter "name=nica-ems") &>/dev/null; sudo docker rm $(sudo docker ps -a -q --filter "name=nica-ems") &>/dev/null
echo '--- Containers have been cleared'
fi
sh gradlew installDist
sudo docker-compose up --build -d

