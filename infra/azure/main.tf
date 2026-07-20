locals {
  base_name           = "${var.name_prefix}-${var.unique_suffix}"
  resource_group_name = "${local.base_name}-rg"
  node_resource_group = "${local.base_name}-nodes-rg"
  acr_name            = substr(lower(replace("${var.name_prefix}${var.unique_suffix}acr", "-", "")), 0, 50)
  dns_label           = substr(lower(replace(local.base_name, "_", "-")), 0, 63)
}

resource "azurerm_resource_group" "main" {
  name     = local.resource_group_name
  location = var.location
  tags     = var.tags
}

resource "azurerm_virtual_network" "main" {
  name                = "${local.base_name}-vnet"
  address_space       = ["10.20.0.0/16"]
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  tags                = var.tags
}

resource "azurerm_subnet" "aks" {
  name                 = "aks-nodes"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = ["10.20.0.0/22"]
}

resource "azurerm_container_registry" "main" {
  name                 = local.acr_name
  resource_group_name  = azurerm_resource_group.main.name
  location             = azurerm_resource_group.main.location
  sku                  = "Basic"
  admin_enabled        = false
  role_assignment_mode = "LegacyRegistryPermissions"
  tags                 = var.tags
}

resource "azurerm_kubernetes_cluster" "main" {
  name                = "${local.base_name}-aks"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  node_resource_group = local.node_resource_group
  dns_prefix          = local.base_name

  role_based_access_control_enabled = true
  local_account_disabled            = true
  oidc_issuer_enabled               = true
  workload_identity_enabled         = true
  automatic_upgrade_channel         = "patch"
  node_os_upgrade_channel           = "NodeImage"
  sku_tier                          = "Free"

  identity {
    type = "SystemAssigned"
  }

  azure_active_directory_role_based_access_control {
    azure_rbac_enabled = true
    tenant_id          = var.tenant_id
  }

  api_server_access_profile {
    authorized_ip_ranges = var.api_server_authorized_ip_ranges
  }

  default_node_pool {
    name                 = "system"
    vm_size              = var.node_vm_size
    vnet_subnet_id       = azurerm_subnet.aks.id
    auto_scaling_enabled = true
    node_count           = var.node_min_count
    min_count            = var.node_min_count
    max_count            = var.node_max_count
    os_sku               = "Ubuntu"
    type                 = "VirtualMachineScaleSets"

    upgrade_settings {
      # AKS system pools require maxUnavailable=0, so retain the Azure default.
      max_surge = "10%"
    }

  }

  network_profile {
    network_plugin      = "azure"
    network_plugin_mode = "overlay"
    network_policy      = "azure"
    load_balancer_sku   = "standard"
    pod_cidr            = "10.244.0.0/16"
    service_cidr        = "10.30.0.0/16"
    dns_service_ip      = "10.30.0.10"
  }

  tags = var.tags

  lifecycle {
    ignore_changes = [default_node_pool[0].node_count]

    precondition {
      condition     = var.node_min_count >= 2 && var.node_max_count >= var.node_min_count
      error_message = "node_min_count must be at least 2 and node_max_count must be greater than or equal to it."
    }
  }
}

resource "azurerm_role_assignment" "aks_admin" {
  for_each = toset(var.aks_admin_user_object_ids)

  scope                = azurerm_kubernetes_cluster.main.id
  role_definition_name = "Azure Kubernetes Service RBAC Cluster Admin"
  principal_id         = each.value
  principal_type       = "User"
}

resource "azurerm_role_assignment" "acr_pull" {
  scope                            = azurerm_container_registry.main.id
  role_definition_name             = "AcrPull"
  principal_id                     = azurerm_kubernetes_cluster.main.kubelet_identity[0].object_id
  skip_service_principal_aad_check = true
}

resource "azurerm_public_ip" "ingress" {
  name                = "${local.base_name}-ingress-pip"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_kubernetes_cluster.main.node_resource_group
  allocation_method   = "Static"
  sku                 = "Standard"
  domain_name_label   = local.dns_label
  tags                = var.tags
}

resource "azurerm_role_assignment" "ingress_public_ip" {
  scope                            = azurerm_public_ip.ingress.id
  role_definition_name             = "Network Contributor"
  principal_id                     = azurerm_kubernetes_cluster.main.identity[0].principal_id
  skip_service_principal_aad_check = true
}
