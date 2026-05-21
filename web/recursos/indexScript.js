document.addEventListener('DOMContentLoaded', () => {
    obtenerRecomendaciones(); 
});

function obtenerRecomendaciones() {
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

                // Mostrar la alerta
                alert('enciende la api tontito');

                // Crear un fondo oscuro para resaltar la imagen
                const errorOverlay = document.createElement('div');
                errorOverlay.style.position = 'fixed';
                errorOverlay.style.top = '0';
                errorOverlay.style.left = '0';
                errorOverlay.style.width = '100vw';
                errorOverlay.style.height = '100vh';
                errorOverlay.style.backgroundColor = 'rgba(0, 0, 0, 0.95)';
                errorOverlay.style.display = 'flex';
                errorOverlay.style.justifyContent = 'center';
                errorOverlay.style.alignItems = 'center';
                errorOverlay.style.zIndex = '9999';

                // Crear y añadir la imagen
                const errorImg = document.createElement('img');
                errorImg.src = 'https://gameboxd.s3.us-east-1.amazonaws.com/web/pistachos.png';
                errorImg.style.maxWidth = '90%';
                errorImg.style.maxHeight = '90%';
                errorImg.style.borderRadius = '20px';
                errorImg.style.boxShadow = '0 0 50px rgba(255, 0, 0, 0.5)'; // Brillo rojo de alerta

                errorOverlay.appendChild(errorImg);
                document.body.appendChild(errorOverlay);
            });
    }