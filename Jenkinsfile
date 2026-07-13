// Template — Job C, uno per ciascun repo Git dentro ms-plugins. Punto d'ingresso principale
// per gli sviluppatori: build manuale ("Build Now") o push sul proprio plugin. Ogni esecuzione
// compila tutta la catena di cui il plugin ha bisogno, sfruttando la cache (Copy Artifact) di
// Job A (idempiere-core) e Job B (idempiere-base) invece di ricompilarli da zero.
//
// Questo file NON viene eseguito da qui: è il modello copiato (con il nome del plugin al posto
// di com.metalsistem.credemsftp) nel Jenkinsfile alla radice di ciascuno dei repo ms-plugins. Le copie reali
// sono già state generate in ciascun repo. Solo com.metalsistem.ufficioacquisti ha una variante
// con uno stage/parametro extraDeps in più (vedi commento in cima al suo Jenkinsfile).

@Library('idempiere-ci@master') _

pipeline {
    agent {
        docker {
            image 'maven:3.9.9-eclipse-temurin-17'
            args '-u root:root -v idempiere-mvn-repo-cache:/root/.m2/repository'
        }
    }

    options {
        timestamps()
    }

    // Niente webhook: Jenkins non è raggiungibile da Internet. pollSCM richiede solo lettura
    // in uscita verso GitHub.
    triggers {
        pollSCM('H/5 * * * *')
    }

    stages {
        stage('Build') {
            steps {
                msPluginBuild(name: 'com.metalsistem.credemsftp')
            }
        }
    }
}
