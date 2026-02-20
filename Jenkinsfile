pipeline {
	agent any

	tools {
		maven 'maven'
	}

	environment {
		SONAR_TOKEN = credentials('sonar-token')

		URL_CADASTRO = "jdbc:postgresql://db-cadastro:5432/cadastro_db"
		URL_TRANSFERENCIA = "jdbc:postgresql://db-transferencia:5432/transferencia_db"
		OIDC_URL = "http://keycloak-estavel:8080/realms/bank-realm"
		KAFKA_URL = "redpanda-estavel:29092"

		DB_USER = "admin"
		DB_PASS = "admin"
	}

	stages {
		stage('Build & Test: Cadastro') {
			steps {
				dir('servico-cadastro') {
					sh "mvn clean verify \
                    -Dquarkus.datasource.jdbc.url=${env.URL_CADASTRO} \
                    -Dquarkus.oidc.auth-server-url=${env.OIDC_URL} \
                    -Dquarkus.datasource.username=${env.DB_USER} \
                    -Dquarkus.datasource.password=${env.DB_PASS} \
                    -Dquarkus.hibernate-orm.generation=update"
				}
			}
		}

		stage('Build & Test: Transferência') {
			steps {
				dir('servico-transferencia') {
					sh "mvn clean verify \
                    -Dquarkus.datasource.jdbc.url=${env.URL_TRANSFERENCIA} \
                    -Dquarkus.oidc.auth-server-url=${env.OIDC_URL} \
                    -Dkafka.bootstrap.servers=${env.KAFKA_URL} \
                    -Dquarkus.datasource.username=${env.DB_USER} \
                    -Dquarkus.datasource.password=${env.DB_PASS} \
                    -Dquarkus.hibernate-orm.generation=drop-and-create"
				}
			}
		}

		stage('SonarQube: Análise de Qualidade') {
			steps {
				script {
					withSonarQubeEnv('SonarQubeServer') {
						// 1. Analisar Serviço de Cadastro
						dir('servico-cadastro') {
							sh "mvn sonar:sonar \
                        -Dsonar.projectKey=bb-cadastro \
                        -Dsonar.projectName='Quarkus Bank - Cadastro' \
                        -Dsonar.java.binaries=target/classes \
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/jacoco-reports/jacoco.xml"
						}

						// 2. Analisar Serviço de Transferência
						dir('servico-transferencia') {
							sh "mvn sonar:sonar \
                        -Dsonar.projectKey=bb-transferencias \
                        -Dsonar.projectName='Quarkus Bank - Transferências' \
                        -Dsonar.java.binaries=target/classes \
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/jacoco-reports/jacoco.xml"
						}
					}
				}
			}
		}

		stage("Check Quality Gate") {
			steps {
				script {
					timeout(time: 1, unit: 'HOURS') {
						def qg = waitForQualityGate()
						if (qg.status != 'OK') {
							error "❌ Quality Gate falhou: ${qg.status}. O build foi abortado para proteger a produção!"
						}
					}
				}
			}
		}

		/* ESTÁGIO COMENTADO PARA POUPAR ESPAÇO EM DISCO
       stage('Docker: Build (Pausado)') {
          steps {
             script {
                echo "Estágio de criação de imagens Docker desativado para economizar armazenamento."
                // dir('servico-cadastro') { sh "docker build -t bb-bank/servico-cadastro:latest ." }
                // dir('servico-transferencia') { sh "docker build -t bb-bank/servico-transferencia:latest ." }
             }
          }
       }
       */

	}
}