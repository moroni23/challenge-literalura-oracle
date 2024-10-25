package com.mtech.mybooks.principal;

import com.fasterxml.jackson.databind.JsonNode;
import com.mtech.mybooks.dto.LivroDTO;
import com.mtech.mybooks.model.Autor;
import com.mtech.mybooks.model.Livro;
import com.mtech.mybooks.repository.LivroRepository;
import com.mtech.mybooks.service.ConsumoApi;
import com.mtech.mybooks.service.ConverteDados;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class Principal {

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private ConsumoApi consumoApi;

    @Autowired
    private ConverteDados converteDados;

    private final Scanner leitura = new Scanner(System.in);


    public Principal(LivroRepository livroRepository, ConsumoApi consumoApi, ConverteDados converteDados) {
        this.livroRepository = livroRepository;
        this.consumoApi = consumoApi;
        this.converteDados = converteDados;
    }

    public void exibeMenu() {
        boolean running = true;
        while (running) {
            exibirMenu();
            var opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1 -> buscarLivrosPeloTitulo();
                case 2 -> listarLivrosRegistrados();
                case 3 -> listarAutoresRegistrados();
                case 4 -> listarAutoresVivos();
                case 5 -> listarAutoresVivosRefinado();
                case 6 -> listarAutoresPorAnoDeMorte();
                case 7 -> listarLivrosPorIdioma();
                case 0 -> {
                    System.out.println("Encerrando o programa!");
                    running = false;
                }
                default -> System.out.println("Opção inválida!");
            }
        }
    }

    private void exibirMenu() {
        System.out.println("""
                *******************************************************************************************
                                    Sejam Bem Vindos ao MyBooks
                       Desenvolvido por: Moroni - Um projeto do Curso Oracle/Alura
                       Explore sobre informações e dados dos seu livros e atores favoritos
                                    Escolha alguma das opções a seguir:
                *******************************************************************************************
                                 ***    Menu  ***
                           1- Buscar livros pelo título
                           2- Listar livros registrados
                           3- Listar autores registrados
                           4- Listar autores vivos em um determinado ano
                           5- Listar autores nascidos em determinado ano
                           6- Listar autores por ano de sua morte
                           7- Listar livros em um determinado idioma
                           0- Sair
                """);
    }

    private void salvarLivros(List<Livro> livros) {
        livros.forEach(livroRepository::save);
    }


    private void buscarLivrosPeloTitulo() {
        String baseURL =  "https://gutendex.com/books/?search=";


        try {
            System.out.println("Digite o título do livro: ");
            String titulo = leitura.nextLine();
            String endereco = baseURL + titulo.replace(" ", "%20");
            System.out.println("URL da API: " + endereco);

            String jsonResponse = consumoApi.obterDados(endereco);
            System.out.println("Resposta da API: " + jsonResponse);

            if (jsonResponse.isEmpty()) {
                System.out.println("Resposta da API está vazia.");
                return;
            }


            JsonNode rootNode = converteDados.getObjectMapper().readTree(jsonResponse);
            JsonNode resultsNode = rootNode.path("results");

            if (resultsNode.isEmpty()) {
                System.out.println("Não foi possível encontrar o livro buscado.");
                return;
            }


            List<LivroDTO> livrosDTO = converteDados.getObjectMapper()
                    .readerForListOf(LivroDTO.class)
                    .readValue(resultsNode);


            List<Livro> livrosExistentes = livroRepository.findByTitulo(titulo);
            if (!livrosExistentes.isEmpty()) {
                System.out.println("Removendo livros duplicados já existentes no banco de dados...");
                for (Livro livroExistente : livrosExistentes) {
                    livrosDTO.removeIf(livroDTO -> livroExistente.getTitulo().equals(livroDTO.titulo()));
                }
            }


            if (!livrosDTO.isEmpty()) {
                System.out.println("Salvando novos livros encontrados...");
                List<Livro> novosLivros = livrosDTO.stream().map(Livro::new).collect(Collectors.toList());
                salvarLivros(novosLivros);
                System.out.println("Livros salvos com sucesso!");
            } else {
                System.out.println("Todos os livros já estão registrados no banco de dados.");
            }


            if (!livrosDTO.isEmpty()) {
                System.out.println("Livros encontrados:");
                Set<String> titulosExibidos = new HashSet<>();
                for (LivroDTO livro : livrosDTO) {
                    if (!titulosExibidos.contains(livro.titulo())) {
                        System.out.println(livro);
                        titulosExibidos.add(livro.titulo());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao buscar livros: " + e.getMessage());
        }
    }


    private void listarLivrosRegistrados() {
        List<Livro> livros = livroRepository.findAll();
        if (livros.isEmpty()) {
            System.out.println("Nenhum livro registrado.");
        } else {
            livros.forEach(System.out::println);
        }
    }

    private void listarAutoresRegistrados() {
        List<Livro> livros = livroRepository.findAll();
        if (livros.isEmpty()) {
            System.out.println("Nenhum autor registrado.");
        } else {
            livros.stream()
                    .map(Livro::getAutor)
                    .distinct()
                    .forEach(autor -> System.out.println(autor.getAutor()));
        }
    }

    private void listarAutoresVivos() {
        System.out.println("Digite o ano: ");
        Integer ano = leitura.nextInt();
        leitura.nextLine();

        Year year = Year.of(ano);

        List<Autor> autores = livroRepository.findAutoresVivos(year);
        if (autores.isEmpty()) {
            System.out.println("Nenhum autor vivo encontrado.");
        } else {
            System.out.println("Lista de autores vivos no ano de " + ano + ":\n");

            autores.forEach(autor -> {
                if (Autor.possuiAno(autor.getAnoNascimento()) && Autor.possuiAno(autor.getAnoFalecimento())) {
                    String nomeAutor = autor.getAutor();
                    String anoNascimento = autor.getAnoNascimento().toString();
                    String anoFalecimento = autor.getAnoFalecimento().toString();
                    System.out.println(nomeAutor + " (" + anoNascimento + " - " + anoFalecimento + ")");
                }
            });
        }
    }

    private void listarAutoresVivosRefinado() {
        System.out.println("Digite o ano: ");
        Integer ano = leitura.nextInt();
        leitura.nextLine();

        Year year = Year.of(ano);

        List<Autor> autores = livroRepository.findAutoresVivosRefinado(year);
        if (autores.isEmpty()) {
            System.out.println("Nenhum autor vivo encontrado.");
        } else {
            System.out.println("Lista de autores nascidos no ano de " + ano + ":\n");

            autores.forEach(autor -> {
                if (Autor.possuiAno(autor.getAnoNascimento()) && Autor.possuiAno(autor.getAnoFalecimento())) {
                    String nomeAutor = autor.getAutor();
                    String anoNascimento = autor.getAnoNascimento().toString();
                    String anoFalecimento = autor.getAnoFalecimento().toString();
                    System.out.println(nomeAutor + " (" + anoNascimento + " - " + anoFalecimento + ")");

                }
            });
        }
    }

    private void listarAutoresPorAnoDeMorte() {
        System.out.println("Digite o ano: ");
        Integer ano = leitura.nextInt();
        leitura.nextLine();

        Year year = Year.of(ano);

        List<Autor> autores = livroRepository.findAutoresPorAnoDeMorte(year);
        if (autores.isEmpty()) {
            System.out.println("Nenhum autor vivo encontrado.");
        } else {

            System.out.println("Lista de autores que morreram no ano de " + ano + ":\n");


            autores.forEach(autor -> {
                if (Autor.possuiAno(autor.getAnoNascimento()) && Autor.possuiAno(autor.getAnoFalecimento())) {
                    String nomeAutor = autor.getAutor();
                    String anoNascimento = autor.getAnoNascimento().toString();
                    String anoFalecimento = autor.getAnoFalecimento().toString();
                    System.out.println(nomeAutor + " (" + anoNascimento + " - " + anoFalecimento + ")");
                }
            });
        }
    }


    private void listarLivrosPorIdioma() {
        System.out.println("""
                Digite o idioma desejado :
                Inglês (en)
                Português (pt)
                Espanhol (es)
                Francês (fr)
                Alemão (de)
                """);
        String idioma = leitura.nextLine();

        List<Livro> livros = livroRepository.findByIdioma(idioma);
        if (livros.isEmpty()) {
            System.out.println("Nenhum livro encontrado.");
        } else {
            livros.forEach(livro -> {
                String titulo = livro.getTitulo();
                String autor = livro.getAutor().getAutor();
                String idiomaLivro = livro.getIdioma();

                System.out.println("Título: " + titulo);
                System.out.println("Autor: " + autor);
                System.out.println("Idioma: " + idiomaLivro);
                System.out.println("----------------------------------------");
            });
        }
    }


}


