package net.freelancertech.vaccation.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.filter.CorsFilter;

import io.github.jhipster.config.JHipsterProperties;
import io.github.jhipster.security.Http401UnauthorizedEntryPoint;
import net.freelancertech.vaccation.security.AuthoritiesConstants;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	private final AuthenticationManagerBuilder authenticationManagerBuilder;

	private final UserDetailsService userDetailsService;

	private final JHipsterProperties jHipsterProperties;

	private final RememberMeServices rememberMeServices;

	private final CorsFilter corsFilter;

	public SecurityConfiguration(AuthenticationManagerBuilder authenticationManagerBuilder,
			@Qualifier("domainUserDetailsService") UserDetailsService userDetailsService,
			JHipsterProperties jHipsterProperties, RememberMeServices rememberMeServices, CorsFilter corsFilter) {

		this.authenticationManagerBuilder = authenticationManagerBuilder;
		this.userDetailsService = userDetailsService;
		this.jHipsterProperties = jHipsterProperties;
		this.rememberMeServices = rememberMeServices;
		this.corsFilter = corsFilter;
	}

	@PostConstruct
	public void init() {
		try {
			authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
		} catch (final Exception e) {
			throw new BeanInitializationException("Security configuration failed", e);
		}
	}

	@Bean
	public Http401UnauthorizedEntryPoint http401UnauthorizedEntryPoint() {
		return new Http401UnauthorizedEntryPoint();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers(HttpMethod.OPTIONS, "/**").antMatchers("/app/**/*.{js,html}").antMatchers("/i18n/**")
				.antMatchers("/content/**").antMatchers("/swagger-ui/index.html").antMatchers("/test/**")
				.antMatchers("/h2-console/**");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http

				.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).and()
				.addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class).exceptionHandling()
				.authenticationEntryPoint(http401UnauthorizedEntryPoint()).and()

				.rememberMe().rememberMeServices(rememberMeServices).rememberMeParameter("remember-me")
				.key(jHipsterProperties.getSecurity().getRememberMe().getKey()).and().formLogin()
				.loginPage("/login.htm").permitAll().loginProcessingUrl("/login.htm")

				.usernameParameter("j_username").passwordParameter("j_password").permitAll().and().logout()
				.logoutUrl("/ui/logout.htm")

				.permitAll().invalidateHttpSession(true)

				.and().authorizeRequests().antMatchers("/api/register").permitAll().antMatchers("/api/activate")
				.permitAll().antMatchers("/api/authenticate").permitAll()
				.antMatchers("/api/account/reset_password/init").permitAll()
				.antMatchers("/api/account/reset_password/finish").permitAll().antMatchers("/api/profile-info")
				.permitAll().antMatchers("/ui/**").authenticated().antMatchers("/api/**").authenticated()
				.antMatchers("/websocket/tracker").hasAuthority(AuthoritiesConstants.ADMIN).antMatchers("/websocket/**")
				.permitAll().antMatchers("/management/health").permitAll().antMatchers("/management/**")
				.hasAuthority(AuthoritiesConstants.ADMIN).antMatchers("/v2/api-docs/**").permitAll()
				.antMatchers("/swagger-resources/configuration/ui").permitAll().antMatchers("/swagger-ui/index.html")
				.hasAuthority(AuthoritiesConstants.ADMIN);

	}

	@Bean
	public SecurityEvaluationContextExtension securityEvaluationContextExtension() {
		return new SecurityEvaluationContextExtension();
	}
}
