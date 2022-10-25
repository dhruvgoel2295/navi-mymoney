package org.mymoney.backend;


import org.mymoney.backend.config.MyMoneyConfig;
import org.mymoney.backend.dao.PortfolioData;
import org.mymoney.backend.service.PortfolioManager;
import org.mymoney.backend.service.impl.DefaultPortfolioService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.InputMismatchException;
import java.util.logging.Level;
import java.util.logging.Logger;

@SpringBootApplication
@EnableAutoConfiguration
public class MyMoneyApplication implements CommandLineRunner {
    static Logger log = Logger.getLogger(MyMoneyApplication.class.getName());
    ApplicationContext context = new AnnotationConfigApplicationContext(MyMoneyConfig.class);
    PortfolioData portfolioData = context.getBean("portfolioData", PortfolioData.class);
    PortfolioManager portfolioManager = new PortfolioManager(new DefaultPortfolioService(portfolioData));

    public static void main(String[] args) {
        SpringApplication.run(MyMoneyApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 1) {
            log.severe("Input Not Found");
            throw new InputMismatchException(
                    "Please specify input file.");
        } else if (args.length > 1) {
            log.severe("More than 1 arguments were supplied");
            throw new InputMismatchException(
                    "Please specify only the input file");
        }
        String input = args[0];
        log.info("Starting Command Processing");
        portfolioManager.executeCommandsFromFile(input);
        System.exit(0);
    }
}