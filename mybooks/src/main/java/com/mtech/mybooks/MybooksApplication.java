package com.mtech.mybooks;

import com.mtech.mybooks.principal.Principal;
import com.mtech.mybooks.repository.LivroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MybooksApplication  implements CommandLineRunner {


	@Autowired
	private Principal principal;


	public static void main(String[] args) {
		SpringApplication.run(MybooksApplication.class, args);
	}




	@Override
	public void run(String... args) throws Exception {
		principal.exibeMenu();
	}
}
