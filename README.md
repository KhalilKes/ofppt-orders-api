# Azure Guide for deployment

### Création groupe de ressources

* az group create --name ecomm-rg --location xxxx

### Créationdes vnet et subnets (pour la segmatetion)
* az network vnet create --resource-group ecomm-rg --name ecomm-vnet --address-prefixes 10.0.0.0/16 
* az network vnet subnet create --resource-group ecomm-rg --vnet-name ecomm-vnet --name public-subnet --address-prefixes 10.0.1.0/24 
* az network vnet subnet create --resource-group ecomm-rg --vnet-name ecomm-vnet --name vm-subnet --address-prefixes 10.0.2.0/24az network vnet subnet create --resource-group ecomm-rg --vnet-name ecomm-vnet --name db-subnet --address-prefixes 10.0.3.0/24

### Création MySQL databases (encryption at rest)
The following guides illustrate how to use some features concretely:

* az mysql flexible-server create --resource-group ecomm-rg --name products-db --location eastus --admin-user dbuser --admin-password StrongPass123! --sku-name Standard_B1ms --tier Burstable --vnet ecomm-vnet --subnet db-subnet --version 8.0 
* az mysql flexible-server create --resource-group ecomm-rg --name orders-db --location eastus --admin-user dbuser --admin-password StrongPass123! --sku-name Standard_B1ms --tier Burstable --vnet ecomm-vnet --subnet db-subnet --version 8.0

### Création VMs
# Products API VM
* az vm create --resource-group ecomm-rg --name products-vm --image UbuntuLTS --size Standard_D2s_v3 --vnet-name ecomm-vnet --subnet vm-subnet --admin-username azureuser --ssh-key-name mykey --no-public-ip --enable-agent true --enable-disk-encryption true
# Orders API VM
az vm create --resource-group ecomm-rg --name orders-vm --image UbuntuLTS --size Standard_D2s_v3 --vnet-name ecomm-vnet --subnet vm-subnet --admin-username azureuser --ssh-key-name mykey --no-public-ip --enable-agent true --enable-disk-encryption true
# Frontend VM
az vm create --resource-group ecomm-rg --name frontend-vm --image UbuntuLTS --size Standard_D2s_v3 --vnet-name ecomm-vnet --subnet vm-subnet --admin-username azureuser --ssh-key-name mykey --no-public-ip --enable-agent true --enable-disk-encryption true

### Network security groups (NSGA)/ZTA
* az network nsg create --resource-group ecomm-rg --name vm-nsg 
* az network nsg rule create --resource-group ecomm-rg --nsg-name vm-nsg --name allow-gateway --priority 100 --source-address-prefixes <gateway-subnet> --destination-address-prefixes 10.0.2.0/24 --destination-port-ranges 8081 8082 80 --protocol Tcp --access Allow 
* az network nsg rule create --resource-group ecomm-rg --nsg-name vm-nsg --name allow-ssh --priority 110 --source-address-prefixes <your-ip> --destination-address-prefixes 10.0.2.0/24 --destination-port-ranges 22 --protocol Tcp --access Allow 
* az network vnet subnet update --resource-group ecomm-rg --vnet-name ecomm-vnet --name vm-subnet --network-security-group vm-nsg

### Gateay avec WAF (reverse proxy)
az network public-ip create --resource-group ecomm-rg --name gateway-ip --allocation-method Static --sku Standard
az network application-gateway create --resource-group ecomm-rg --name ecomm-gateway --location eastus --public-ip-address gateway-ip --vnet-name ecomm-vnet --subnet public-subnet --capacity 2 --sku WAF_v2 --priority 1

az network application-gateway address-pool create --resource-group ecomm-rg --gateway-name ecomm-gateway --name products-pool --servers <products-vm-private-ip>
az network application-gateway address-pool create --resource-group ecomm-rg --gateway-name ecomm-gateway --name orders-pool --servers <orders-vm-private-ip>
az network application-gateway address-pool create --resource-group ecomm-rg --gateway-name ecomm-gateway --name frontend-pool --servers <frontend-vm-private-ip>

### lister les IP 
az vm list-ip-addresses --resource-group ecomm-rg

### routing
az network application-gateway rule create --resource-group ecomm-rg --gateway-name ecomm-gateway --name products-rule --http-listener <frontend-listener> --address-pool products-pool --http-settings products-settings --rule-type PathBasedRouting --path-rule /products/*
az network application-gateway rule create --resource-group ecomm-rg --gateway-name ecomm-gateway --name orders-rule --http-listener <frontend-listener> --address-pool orders-pool --http-settings orders-settings --rule-type PathBasedRouting --path-rule /orders/* /login/*
az network application-gateway rule create --resource-group ecomm-rg --gateway-name ecomm-gateway --name frontend-rule --http-listener <frontend-listener> --address-pool frontend-pool --http-settings frontend-settings --rule-type Basic

### WAF
az network application-gateway waf-config set --enabled true --firewall-mode Prevention --rule-set-version 3.2 --gateway-name ecomm-gateway --resource-group ecomm-rg

### Azure Defender
az security pricing create --name CloudPosture --tier Standard

sudo nano /etc/systemd/system/products-api.service

[Unit]
Description=Products API
After=network.target
[Service]
User=azureuser
ExecStart=/usr/bin/java -jar /home/azureuser/app.jar
Restart=always
[Install]
WantedBy=multi-user.target

sudo systemctl enable products-api
sudo systemctl start products-api


### Configuration Nginx:
sudo nano /etc/nginx/sites-available/default
nginxserver {
listen 80;
server_name _;
root /home/azureuser/ecomm-frontend;
index index.html;
location / {
try_files $uri $uri/ /index.html;
}
location /products/ {
proxy_pass http://<products-vm-private-ip>:8081/;
}
location /orders/ {
proxy_pass http://<orders-vm-private-ip>:8082/;
}
location /login/ {
proxy_pass http://<orders-vm-private-ip>:8082/;
}
}

Replace <products-vm-private-ip> and <orders-vm-private-ip>.


Restart Nginx:
bashsudo systemctl restart nginx
sudo systemctl enable nginx

### fireall restriction
az mysql flexible-server firewall-rule create --resource-group ecomm-rg --name products-db --rule-name allow-vms --start-ip-address 10.0.2.0 --end-ip-address 10.0.2.255
az mysql flexible-server firewall-rule create --resource-group ecomm-rg --name orders-db --rule-name allow-vms --start-ip-address 10.0.2.0 --end-ip-address 10.0.2.255











