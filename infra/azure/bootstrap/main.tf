resource "random_string" "storage_suffix" {
  length  = 10
  upper   = false
  special = false
}

resource "azurerm_resource_group" "state" {
  name     = "${var.name_prefix}-tfstate-rg"
  location = var.location
  tags     = var.tags
}

resource "azurerm_storage_account" "state" {
  name                          = substr(lower(replace("${var.name_prefix}${random_string.storage_suffix.result}tf", "-", "")), 0, 24)
  resource_group_name           = azurerm_resource_group.state.name
  location                      = azurerm_resource_group.state.location
  account_tier                  = "Standard"
  account_replication_type      = "LRS"
  min_tls_version               = "TLS1_2"
  public_network_access_enabled = true
  # The bootstrap provider uses the account key to create the first container.
  # The platform backend itself is configured for Microsoft Entra authentication.
  shared_access_key_enabled = true
  tags                      = var.tags

  blob_properties {
    versioning_enabled = true

    delete_retention_policy {
      days = 7
    }

    container_delete_retention_policy {
      days = 7
    }
  }
}

resource "azurerm_storage_container" "state" {
  name                  = "tfstate"
  storage_account_id    = azurerm_storage_account.state.id
  container_access_type = "private"
}
