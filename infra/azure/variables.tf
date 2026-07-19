variable "subscription_id" {
  description = "Azure subscription in which the Team Drops resources will be created."
  type        = string
}

variable "location" {
  description = "Azure region for the deployment."
  type        = string
  default     = "westeurope"
}

variable "name_prefix" {
  description = "Lowercase prefix used for Azure resource names."
  type        = string
  default     = "team-drops"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{2,20}$", var.name_prefix))
    error_message = "name_prefix must be 3-21 lowercase letters, digits, or hyphens and start with a letter."
  }
}

variable "unique_suffix" {
  description = "Globally unique lowercase suffix used by ACR and the public DNS label."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9]{4,12}$", var.unique_suffix))
    error_message = "unique_suffix must contain 4-12 lowercase letters or digits."
  }
}

variable "aks_admin_group_object_ids" {
  description = "Microsoft Entra group object IDs that receive AKS administrator access."
  type        = list(string)

  validation {
    condition     = length(var.aks_admin_group_object_ids) > 0
    error_message = "Provide at least one Microsoft Entra administrator group object ID."
  }
}

variable "api_server_authorized_ip_ranges" {
  description = "Public CIDR ranges allowed to reach the AKS API server."
  type        = list(string)

  validation {
    condition     = length(var.api_server_authorized_ip_ranges) > 0
    error_message = "Provide at least one trusted CIDR for AKS API access."
  }
}

variable "node_vm_size" {
  description = "VM size for the autoscaling AKS node pool."
  type        = string
  default     = "Standard_D4s_v5"
}

variable "node_min_count" {
  description = "Minimum AKS node count."
  type        = number
  default     = 2
}

variable "node_max_count" {
  description = "Maximum AKS node count."
  type        = number
  default     = 4
}

variable "tags" {
  description = "Tags added to Azure resources."
  type        = map(string)
  default = {
    application = "team-drops"
    environment = "azure"
    managed-by  = "terraform"
  }
}
