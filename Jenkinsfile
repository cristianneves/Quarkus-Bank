pipeline {
	agent any

	tools {
		// O nome aqui deve ser IDÊNTICO ao que você colocou no Passo 1
		maven 'maven'
	}

	environment {
		SONAR_TOKEN = credentials('sonar-token')
		// 🚀 IPs para o Jenkins (que está dentro do Docker) falar com os serviços
		DB_URL = "jdbc:postgresql://172.17.0.1:5499/bank_db"
		KAFKA_HOST = "172.17.0.1:9092"
	}

	stages {
		stage('Análise de Alterações') {
			steps {
				echo 'Iniciando pipeline multi-serviço...'
			}
		}

		stage('Build & Test: Cadastro') {
			steps {
				dir('servico-cadastro') {
					sh "mvn clean verify -Dquarkus.datasource.jdbc.url=${env.DB_URL} -Dquarkus.datasource.username=quarkus -Dquarkus.datasource.password=quarkus -Dquarkus.hibernate-orm.database.generation=update"
				}
			}
		}

		stage('Build & Test: Transferência') {
			steps {
				dir('servico-transferencia') {
					sh "mvn clean verify -Dquarkus.datasource.jdbc.url=${env.DB_URL} -Dquarkus.datasource.username=quarkus -Dquarkus.datasource.password=quarkus"
				}
			}
		}

		stage('SonarQube: Analisar Tudo') {
			steps {
				// Aqui rodamos a análise na raiz ou por serviço
				dir('servico-transferencia') {
					script {
						withSonarQubeEnv('SonarQubeServer') {
							sh "mvn sonar:sonar -Dsonar.projectKey=bb-transferencias"
						}
					}
				}
			}
		}
	}
}