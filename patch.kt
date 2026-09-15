                var expanded by remember { mutableStateOf(false) }
                
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { expanded = true }) {
                        Text(viewModel.supportedLanguages[targetLanguage] ?: "Select", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        viewModel.supportedLanguages.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name, fontWeight = if (code == targetLanguage) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { 
                                    targetLanguage = code
                                    viewModel.setTargetLanguage(code) 
                                    expanded = false 
                                }
                            )
                        }
                    }
                }
