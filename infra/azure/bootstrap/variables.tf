variable "subscription_id" {
  description = "Azure subscription in which the Terraform state storage is created."
  type        = string
}

variable "state_admin_user_object_id" {
  description = "Microsoft Entra user object ID allowed to read and write Terraform state blobs."
  type        = string
}

variable "location" {
  description = "Azure region for Terraform state storage."
  type        = string
  default     = "westeurope"
}

variable "name_prefix" {
  description = "Prefix used for state resources."
  type        = string
  default     = "team-drops"
}

variable "tags" {
  description = "Tags added to state resources."
  type        = map(string)
  default = {
    application = "team-drops"
    purpose     = "terraform-state"
    managed-by  = "terraform"
  }
}
