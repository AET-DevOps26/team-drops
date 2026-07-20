output "resource_group_name" {
  description = "Resource group containing AKS and ACR."
  value       = azurerm_resource_group.main.name
}

output "aks_cluster_name" {
  description = "AKS cluster name."
  value       = azurerm_kubernetes_cluster.main.name
}

output "aks_node_resource_group" {
  description = "AKS-managed node resource group containing the ingress public IP."
  value       = azurerm_kubernetes_cluster.main.node_resource_group
}

output "acr_login_server" {
  description = "Azure Container Registry login server."
  value       = azurerm_container_registry.main.login_server
}

output "ingress_public_ip_name" {
  description = "Name of the reserved ingress public IP."
  value       = azurerm_public_ip.ingress.name
}

output "ingress_public_ip" {
  description = "Reserved ingress IPv4 address."
  value       = azurerm_public_ip.ingress.ip_address
}

output "application_hostname" {
  description = "Azure-provided hostname assigned to the ingress public IP."
  value       = azurerm_public_ip.ingress.fqdn
}

output "get_credentials_command" {
  description = "Manual command for retrieving AKS credentials."
  value       = "az aks get-credentials --resource-group ${azurerm_resource_group.main.name} --name ${azurerm_kubernetes_cluster.main.name} --overwrite-existing"
}
