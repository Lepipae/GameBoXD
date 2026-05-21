document.addEventListener('DOMContentLoaded', () => {
    // URL de la API de videojuegos de Spring Boot
    const API_URL = 'http://localhost:8080/api/videojuegos';

    fetch(API_URL)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error de red al intentar obtener los videojuegos');
            }
            return response.json();
        })
        .then(juegos => {
            // Primero, filtramos quedándonos solo con los juegos que tienen una imagen válida
            const juegosConImagen = juegos.filter(juego => juego.urlImagen && juego.urlImagen.trim() !== '');

            // Queremos sacar 4 recomendaciones (o el máximo disponible si hay menos de 4)
            const cantidadNecesaria = Math.min(4, juegosConImagen.length);
            const recomendaciones = [];

            // Seleccionamos al azar sin repetir
            while (recomendaciones.length < cantidadNecesaria) {
                const indiceAleatorio = Math.floor(Math.random() * juegosConImagen.length);
                const juegoCandidato = juegosConImagen[indiceAleatorio];
                
                // Si aún no está en nuestra lista de recomendaciones, lo añadimos
                if (!recomendaciones.includes(juegoCandidato)) {
                    recomendaciones.push(juegoCandidato);
                }
            }
            
            // Insertamos las imágenes en el HTML
            recomendaciones.forEach((juego, index) => {
                const contenedorId = `r${index + 1}`;
                const contenedor = document.getElementById(contenedorId);
                
                if (contenedor) {
                    const img = document.createElement('img');
                    img.src = juego.urlImagen;
                    img.alt = juego.nombre;
                    img.title = juego.nombre; 
                    
                    contenedor.appendChild(img);
                }
            });
        })
        .catch(error => {
            console.error('Error fetching video games:', error);
        });
});